<div class="header">
    <div class="container-fluid">
        <a ng-click="removeCurrentModule()" href="."><div class="dashboard-logo" ng-show="showDashboardLogo.showDashboard"></div></a>
        <div class="header-title" ng-show="showDashboardLogo.showDashboard">{{msg('motechTitle')}}</div>
    </div>
</div>
<div class="clearfix"></div>

<div class="header-nav navbar">
    <div class="navbar-inner navbar-inverse navbar-inner-bg">
        <div class="container-fluid">
            <a class="btn btn-navbar btn-blue" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </a>
            <a id="brand" class="brand" ng-hide="showDashboardLogo.showDashboard" href="#">MOTECH</a>

            <div class="nav-collapse">
                <ul class="nav" role="navigation">
                    <li class="divider-vertical" ng-hide="showDashboardLogo.showDashboard" ></li>
                    <li class="current"><a role="menu" ng-click="removeCurrentModule()" href=".">{{msg('home')}}</a></li>
                    <li class="divider-vertical divider-vertical-sub"></li>
                    <li><a role="menu">{{msg('motech')}} {{msg('project')}}</a></li>
                    <li class="divider-vertical divider-vertical-sub"></li>
                    <li><a role="menu">{{msg('community')}}</a></li>
                </ul>
                <a id="minimize" class="btn btn-mini btn-blue" ng-click="minimizeHeader()">
                    <img src="resources/img/trans.gif" title="{{msg(showDashboardLogo.changeTitle())}}"
                         alt="{{msg(showDashboardLogo.changeTitle())}}"
                         ng-class="showDashboardLogo.changeClass()"/>
                </a>
                <ul class="nav pull-right menu-left">

                    <li class="dropdown">
                        <a class="dropdown-toggle" href="#" data-toggle="dropdown">
                            <span ui-if="!user.anonymous">{{msg('loggedAs')}} <strong>{{ user.userName }}</strong><strong class="caret"></strong></span>
                            <span ui-if="user.anonymous">{{msg('welcome')}} <strong>{{ msg('anonymous') }}</strong><strong class="caret"></strong></span>
                        </a>
                        <ul id="localization" class="dropdown-menu" role="menu">
                            <li ui-if="user.securityLaunch">
                                <a href="home?moduleName=websecurity#/profile/{{user.userName}}" tabindex="-1">
                                    <i class="icon-user"></i> {{msg('profile')}}
                                </a>
                            </li>
                            <li ui-if="user.securityLaunch" class="divider"></li>
                            <li class="dropdown-submenu pull-left">
                                <a class="menu-flag dropdown-toggle" tabindex="-1" data-toggle="dropdown" href="#">
                                    <i class="flag flag-{{userLang.key}}" title="{{userLang.key}}" alt="{{userLang.key}}"></i>
                                    <span class="text-capitalize">{{languages[userLang.key]}}</span>
                                </a>
                                <ul class="dropdown-menu">
                                    <li ng-repeat="(key, value) in languages">
                                        <a ng-click="setUserLang(key)"><i class="flag flag-{{key}}"></i> {{value}}</a>
                                    </li>
                                </ul>
                            </li>
                            <li ui-if="user.securityLaunch" class="divider"></li>
                            <li ui-if="user.securityLaunch">
                                <a href="{{contextPath}}j_spring_security_logout" class="">
                                    <i class="icon-off"></i> {{msg('signOut')}}
                                </a>
                            </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</div>

<div class="clearfix"></div>
