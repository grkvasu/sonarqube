/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileRef;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeParentActionMediumTest {

  // TODO Replace with DbTester + EsTester once DaoV2 is removed
  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db;
  private DbSession session;
  private WsTester wsTester;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private RuleIndex ruleIndex;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    ruleIndex = tester.get(RuleIndex.class);
    userSessionRule.logIn().addOrganizationPermission(tester.get(DefaultOrganizationProvider.class).get().getUuid(), GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void change_parent_with_no_parent_before() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent1);
    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(db.activeRuleDao().selectByProfileKey(session, child.getKey())).isEmpty();

    // Set parent
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", parent1.getKey())
      .execute();
    session.clearCache();

    // Check rule 1 enabled
    List<ActiveRuleDto> activeRules1 = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().ruleKey().rule()).isEqualTo("rule1");

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void replace_existing_parent() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto parent2 = createProfile("xoo", "Parent 2");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    // Set parent 1
    tester.get(RuleActivator.class).setParent(session, child.getKey(), parent1.getKey());
    session.clearCache();

    // Set parent 2 through WS
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", parent2.getKey())
      .execute();
    session.clearCache();

    // Check rule 2 enabled
    List<ActiveRuleDto> activeRules2 = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().ruleKey().rule()).isEqualTo("rule2");

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void remove_parent() throws Exception {
    QualityProfileDto parent = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent);
    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    // Set parent
    tester.get(RuleActivator.class).setParent(session, child.getKey(), parent.getKey());
    session.clearCache();

    // Remove parent through WS
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKey())
      .execute();
    session.clearCache();

    // Check no rule enabled
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules).isEmpty();

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void change_parent_with_names() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto parent2 = createProfile("xoo", "Parent 2");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(db.activeRuleDao().selectByProfileKey(session, child.getKey())).isEmpty();

    // 1. Set parent 1
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_LANGUAGE, "xoo")
      .setParam(QProfileRef.PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", parent1.getName())
      .execute();
    session.clearCache();

    // 1. check rule 1 enabled
    List<ActiveRuleDto> activeRules1 = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().ruleKey().rule()).isEqualTo("rule1");
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);

    // 2. Set parent 2
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_LANGUAGE, "xoo")
      .setParam(QProfileRef.PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", parent2.getName())
      .execute();
    session.clearCache();

    // 2. check rule 2 enabled
    List<ActiveRuleDto> activeRules2 = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().ruleKey().rule()).isEqualTo("rule2");

    // 3. Remove parent
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_LANGUAGE, "xoo")
      .setParam(QProfileRef.PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", "")
      .execute();
    session.clearCache();

    // 3. check no rule enabled
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void remove_parent_with_empty_key() throws Exception {
    QualityProfileDto parent = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent);
    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(db.activeRuleDao().selectByProfileKey(session, child.getKey())).isEmpty();

    // Set parent
    tester.get(RuleActivator.class).setParent(session, child.getKey(), parent.getKey());
    session.clearCache();

    // Remove parent
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", "")
      .execute();
    session.clearCache();

    // Check no rule enabled
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(session, child.getKey());
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_parent_key_and_name_both_set() throws Exception {
    QualityProfileDto child = createProfile("xoo", "Child");
    session.commit();

    assertThat(db.activeRuleDao().selectByProfileKey(session, child.getKey())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKee())
      .setParam("parentName", "polop")
      .setParam("parentKey", "palap")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_profile_key_and_name_both_set() throws Exception {
    QualityProfileDto child = createProfile("xoo", "Child");
    session.commit();

    assertThat(db.activeRuleDao().selectByProfileKey(session, child.getKey())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, child.getKee())
      .setParam(QProfileRef.PARAM_PROFILE_NAME, child.getName())
      .setParam("parentKey", "palap")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_if_missing_permission() throws Exception {
    userSessionRule.logIn("anakin");
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "change_parent")
      .setParam(QProfileRef.PARAM_PROFILE_KEY, "polop")
      .setParam("parentKey", "pulup")
      .execute();
  }

  private QualityProfileDto createProfile(String lang, String name) {
    QualityProfileDto profile = QProfileTesting.newQProfileDto(new QProfileName(lang, name), "p" + lang + "-" + name.toLowerCase());
    db.qualityProfileDao().insert(session, profile);
    return profile;
  }

  private RuleDto createRule(String lang, String id) {
    RuleDto rule = RuleTesting.newDto(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    db.ruleDao().insert(session, rule);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QualityProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }
}
