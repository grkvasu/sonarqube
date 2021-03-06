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
import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Account from './components/Account';
import ProjectsContainer from './projects/ProjectsContainer';
import Security from './components/Security';
import Profile from './profile/Profile';
import Notifications from './notifications/Notifications';
import UserOrganizations from './organizations/UserOrganizations';
import CreateOrganizationForm from './organizations/CreateOrganizationForm';

export default (
    <Route component={Account}>
      <IndexRoute component={Profile}/>
      <Route path="security" component={Security}/>
      <Route path="projects" component={ProjectsContainer}/>
      <Route path="notifications" component={Notifications}/>
      <Route path="organizations" component={UserOrganizations}>
        <Route path="create" component={CreateOrganizationForm}/>
      </Route>

      <Route path="issues" onEnter={() => {
        window.location = window.baseUrl + '/issues' + window.location.hash + '|assigned_to_me=true';
      }}/>
    </Route>
);
