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
/**
 * Generate URL for a component's home page
 * @param {string} componentKey
 * @returns {string}
 */
export function getComponentUrl (componentKey) {
  return window.baseUrl + '/dashboard?id=' + encodeURIComponent(componentKey);
}

export function getProjectUrl (key) {
  return {
    pathname: '/dashboard',
    query: { id: key }
  };
}

/**
 * Generate URL for a global issues page
 * @param {object} query
 * @returns {string}
 */
export function getIssuesUrl (query) {
  const serializedQuery = Object.keys(query).map(criterion => (
      `${encodeURIComponent(criterion)}=${encodeURIComponent(query[criterion])}`
  )).join('|');

  // return a string (not { pathname }) to help react-router's Link handle this properly
  return '/issues#' + serializedQuery;
}

/**
 * Generate URL for a component's issues page
 * @param {string} componentKey
 * @param {object} query
 * @returns {string}
 */
export function getComponentIssuesUrl (componentKey, query) {
  const serializedQuery = Object.keys(query).map(criterion => (
      `${encodeURIComponent(criterion)}=${encodeURIComponent(query[criterion])}`
  )).join('|');

  // return a string (not { pathname }) to help react-router's Link handle this properly
  return '/component_issues?id=' + encodeURIComponent(componentKey) + '#' + serializedQuery;
}

/**
 * Generate URL for a component's drilldown page
 * @param {string} componentKey
 * @param {string} metric
 * @returns {Object}
 */
export function getComponentDrilldownUrl (componentKey, metric) {
  return {
    pathname: `/component_measures/metric/${metric}`,
    query: { id: componentKey }
  };
}

/**
 * Generate URL for a component's permissions page
 * @param {string} componentKey
 * @returns {Object}
 */
export function getComponentPermissionsUrl (componentKey) {
  return {
    pathname: '/project_roles',
    query: { id: componentKey }
  };
}

/**
 * Generate URL for a quality profile
 * @param {string} key
 * @returns {Object}
 */
export function getQualityProfileUrl (key) {
  return {
    pathname: '/profiles/show',
    query: { key }
  };
}

/**
 * Generate URL for a quality gate
 * @param {string} key
 * @returns {Object}
 */
export function getQualityGateUrl (key) {
  return {
    pathname: '/quality_gates/show/' + encodeURIComponent(key)
  };
}

/**
 * Generate URL for the rules page
 * @param {object} query
 * @returns {string}
 */
export function getRulesUrl (query) {
  if (query) {
    const serializedQuery = Object.keys(query).map(criterion => (
        `${encodeURIComponent(criterion)}=${encodeURIComponent(
            query[criterion])}`
    )).join('|');

    // return a string (not { pathname }) to help react-router's Link handle this properly
    return '/coding_rules#' + serializedQuery;
  }

  return '/coding_rules';
}

/**
 * Generate URL for the rules page filtering only active deprecated rules
 * @param {object} query
 * @returns {string}
 */
export function getDeprecatedActiveRulesUrl (query = {}) {
  const baseQuery = { activation: 'true', statuses: 'DEPRECATED' };
  return getRulesUrl({ ...query, ...baseQuery });
}

export const getProjectsUrl = () => {
  return window.baseUrl + '/projects';
};
