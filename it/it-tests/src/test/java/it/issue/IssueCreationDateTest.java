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
package it.issue;

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractDateAssert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

/**
 * @see <a href="https://jira.sonarsource.com/browse/MMF-567">MMF-567</a>
 */
public class IssueCreationDateTest extends AbstractIssueTest {

  private static final String SAMPLE_1_DIRECTORY = "issue/creationDateSample1";
  private static final String SAMPLE_1_PROJECT_KEY = "creation-date-sample-1";
  private static final String SAMPLE_1_PROJECT_NAME = "Creation date Sample 1";

  // quality profiles
  private static final String WITH_RULE = "/issue/IssueCreationTest/with-custom-message.xml";
  private static final String WITHOUT_RULE = "/issue/IssueCreationTest/empty.xml";

  // scm settings
  private static final Consumer<SonarScanner> WITH_SCM = scanner -> {
    scanner
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false");
  };
  private static final Consumer<SonarScanner> WITHOUT_SCM = scanner -> {
  };

  private static final int IN_THE_RANGE_OF_ONE_MINUTE = 60_000;

  private Server server = ORCHESTRATOR.getServer();

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();
    server.provisionProject(SAMPLE_1_PROJECT_KEY, SAMPLE_1_PROJECT_NAME);
  }

  @Test
  public void scm_date_should_be_set_if_scm_date_is_available() throws ParseException {
    assertThatCreationDate(of_analysis(WITH_SCM, WITH_RULE))
      .isInSameSecondAs(scmDate());
  }

  @Test
  public void current_date_should_be_set_if_no_scm_date_is_available() throws ParseException {
    assertThatCreationDate(of_analysis(WITHOUT_SCM, WITH_RULE))
      .isCloseTo(now(), IN_THE_RANGE_OF_ONE_MINUTE);
  }

  @Test
  public void use_scm_date_for_newly_added_rules() throws ParseException {
    assertThat(of_analysis(WITHOUT_SCM, WITHOUT_RULE))
      .isEmpty();

    assertThatCreationDate(of_analysis(WITH_SCM, WITH_RULE))
      .isInSameSecondAs(scmDate());
  }

  private List<Issue> of_analysis(Consumer<SonarScanner> scm, String profile) {
    server.restoreProfile(FileLocation.ofClasspath(profile));
    server.associateProjectToQualityProfile(SAMPLE_1_PROJECT_KEY, "xoo", "with-custom-message");

    SonarScanner scanner = SonarScanner.create(projectDir(SAMPLE_1_DIRECTORY));
    scm.accept(scanner);
    ORCHESTRATOR.executeBuild(scanner);

    return issueClient().find(IssueQuery.create()).list();
  }

  private AbstractDateAssert<?> assertThatCreationDate(List<Issue> issues) {
    return assertThat(issues.get(0).creationDate());
  }

  private static String scmDate() {
    return "2013-01-04T01:00:00.000";
  }

  private static Date now() {
    return new Date();
  }
}
