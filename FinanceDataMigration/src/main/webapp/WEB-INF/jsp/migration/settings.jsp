<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Migration Upload</title>
<%@ include file="../layout/head.jsp"%>
<link
	href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css"
	rel="stylesheet" />
</head>

<body>
	<!-- HEADER -->
	<%@ include file="../layout/header.jsp"%>

	<!-- NAVBAR -->
	<%@ include file="../layout/navbar.jsp"%>

<div class="settings-page">

    <!-- =====================================================
         HEADER
         ===================================================== -->

    <section class="settings-header">

        <div>

            <div class="settings-eyebrow">

                <i class="fa-solid fa-gear"></i>

                ADMINISTRATION

            </div>

            <h1>
                Settings & Administration
            </h1>

            <p>
                Manage migration framework configuration,
                tenants, modules and system information.
            </p>

        </div>

    </section>


    <!-- =====================================================
         SYSTEM STATUS
         ===================================================== -->

    <section class="settings-status-grid">


        <div class="settings-status-card">

            <div class="settings-status-icon green">

                <i class="fa-solid fa-server"></i>

            </div>

            <div>

                <span>
                    Application
                </span>

                <strong id="applicationStatus">
                    Checking...
                </strong>

            </div>

            <span
                class="status-dot green"
                id="applicationStatusDot">
            </span>

        </div>


        <div class="settings-status-card">

            <div class="settings-status-icon blue">

                <i class="fa-solid fa-database"></i>

            </div>

            <div>

                <span>
                    Database
                </span>

                <strong id="databaseStatus">
                    Checking...
                </strong>

            </div>

            <span
                class="status-dot"
                id="databaseStatusDot">
            </span>

        </div>


        <div class="settings-status-card">

            <div class="settings-status-icon purple">

                <i class="fa-solid fa-gears"></i>

            </div>

            <div>

                <span>
                    Migration Engine
                </span>

                <strong id="migrationEngineStatus">
                    Checking...
                </strong>

            </div>

            <span
                class="status-dot green"
                id="migrationEngineStatusDot">
            </span>

        </div>


        <div class="settings-status-card">

            <div class="settings-status-icon orange">

                <i class="fa-solid fa-spinner"></i>

            </div>

            <div>

                <span>
                    Active Jobs
                </span>

                <strong id="activeJobs">
                    0
                </strong>

            </div>

        </div>

    </section>


    <!-- =====================================================
         MAIN GRID
         ===================================================== -->

    <section class="settings-grid">


        <!-- =================================================
             MIGRATION CONFIGURATION
             ================================================= -->

        <div class="settings-panel">

            <div class="settings-panel-header">

                <div class="settings-panel-icon blue">

                    <i class="fa-solid fa-sliders"></i>

                </div>

                <div>

                    <h3>
                        Migration Configuration
                    </h3>

                    <p>
                        Framework-wide migration settings.
                    </p>

                </div>

            </div>


            <div class="settings-list">

                <div class="settings-row">

                    <div>

                        <strong>
                            Enabled Modules
                        </strong>

                        <small>
                            Modules available in the framework
                        </small>

                    </div>

                    <span
                        class="settings-value"
                        id="totalModules">
                        0
                    </span>

                </div>


                <div class="settings-row">

                    <div>

                        <strong>
                            Default Page Size
                        </strong>

                        <small>
                            Number of records shown per page
                        </small>

                    </div>

                    <span
                        class="settings-value"
                        id="defaultPageSize">
                        10
                    </span>

                </div>


                <div class="settings-row">

                    <div>

                        <strong>
                            Auto Refresh
                        </strong>

                        <small>
                            Dashboard/history refresh interval
                        </small>

                    </div>

                    <span
                        class="settings-value"
                        id="autoRefresh">
                        10 sec
                    </span>

                </div>

                <div class="settings-row">

                    <div>

                        <strong>
                            Duplicate Detection
                        </strong>

                        <small>
                            Duplicate records are skipped during migration
                        </small>

                    </div>

                    <span class="settings-enabled">
                        Enabled
                    </span>

                </div>

            </div>

        </div>


        <!-- =================================================
             TENANTS
             ================================================= -->

        <div class="settings-panel">

            <div class="settings-panel-header">

                <div class="settings-panel-icon green">

                    <i class="fa-solid fa-building"></i>

                </div>

                <div>

                    <h3>
                        Tenant Configuration
                    </h3>

                    <p>
                        Tenants currently configured for migration.
                    </p>

                </div>

            </div>


            <div class="tenant-summary">

                <div class="tenant-count">

                    <strong id="tenantCount">
                        0
                    </strong>

                    <span>
                        Configured Tenants
                    </span>

                </div>

            </div>


            <div
			    class="tenant-list"
			    id="tenantList">
			
			    <div class="settings-loading">
			
			        <i class="fa-solid fa-spinner fa-spin"></i>
			
			        Loading tenants...
			
			    </div>
			
			</div>

            <div class="settings-note">

                <i class="fa-solid fa-circle-info"></i>

                Tenant configuration is managed through
                <strong>finance.tenants</strong>.

            </div>

        </div>


        <!-- =================================================
             MODULES
             ================================================= -->

        <div class="settings-panel">

            <div class="settings-panel-header">

                <div class="settings-panel-icon purple">

                    <i class="fa-solid fa-layer-group"></i>

                </div>

                <div>

                    <h3>
                        Migration Modules
                    </h3>

                    <p>
                        Modules available in this framework.
                    </p>

                </div>

            </div>


            <div
			    class="module-list"
			    id="moduleList">
			
			    <div class="settings-loading">
			
			        <i class="fa-solid fa-spinner fa-spin"></i>
			
			        Loading modules...
			
			    </div>
			
			</div>

        </div>


        <!-- =================================================
             APPLICATION INFORMATION
             ================================================= -->

        <div class="settings-panel">

            <div class="settings-panel-header">

                <div class="settings-panel-icon orange">

                    <i class="fa-solid fa-circle-info"></i>

                </div>

                <div>

                    <h3>
                        Application Information
                    </h3>

                    <p>
                        Framework runtime information.
                    </p>

                </div>

            </div>


            <div class="settings-list">


			    <!-- PAGE SIZE -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Default Page Size
			            </strong>
			
			            <small>
			                Number of records shown per page
			            </small>
			
			        </div>
			
			
			        <input
			            type="number"
			            id="defaultPageSize"
			            class="settings-input"
			            min="5"
			            max="100">
			
			    </div>
			
			
			    <!-- AUTO REFRESH -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Auto-refresh Interval
			            </strong>
			
			            <small>
			                Dashboard and history refresh interval
			            </small>
			
			        </div>
			
			
			        <div class="settings-input-group">
			
			            <input
			                type="number"
			                id="autoRefreshSeconds"
			                class="settings-input"
			                min="5"
			                max="300">
			
			            <span>
			                sec
			            </span>
			
			        </div>
			
			    </div>
			
			
			    <!-- UPLOAD SIZE -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Maximum Upload Size
			            </strong>
			
			            <small>
			                Maximum Excel file size
			            </small>
			
			        </div>
			
			
			        <div class="settings-input-group">
			
			            <input
			                type="number"
			                id="maxUploadSizeMb"
			                class="settings-input"
			                min="1"
			                max="500">
			
			            <span>
			                MB
			            </span>
			
			        </div>
			
			    </div>
			
			
			    <!-- EXTENSIONS -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Allowed File Extensions
			            </strong>
			
			            <small>
			                Comma separated file extensions
			            </small>
			
			        </div>
			
			
			        <input
			            type="text"
			            id="allowedFileExtensions"
			            class="settings-input extensions-input"
			            placeholder="xlsx,xls">
			
			    </div>
			
			
			    <!-- DUPLICATE -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Duplicate Policy
			            </strong>
			
			            <small>
			                How duplicate records are handled
			            </small>
			
			        </div>
			
			
			        <select
			            id="duplicatePolicy"
			            class="settings-input">
			
			            <option value="SKIP">
			                Skip
			            </option>
			
			            <option value="FAIL">
			                Fail Migration
			            </option>
			
			            <option value="ALLOW">
			                Allow
			            </option>
			
			        </select>
			
			    </div>
			
			
			    <!-- CONCURRENT -->
			
			    <div class="settings-edit-row">
			
			        <div class="settings-edit-info">
			
			            <strong>
			                Concurrent Migration Limit
			            </strong>
			
			            <small>
			                Maximum migrations running simultaneously
			            </small>
			
			        </div>
			
			
			        <input
			            type="number"
			            id="concurrentMigrationLimit"
			            class="settings-input"
			            min="1"
			            max="20">
			
			    </div>
			
			
			    <!-- ACTIONS -->
			
			    <div class="settings-actions">
			
			        <button
			            type="button"
			            id="resetSettingsBtn"
			            class="settings-button secondary">
			
			            <i class="fa-solid fa-rotate-left"></i>
			
			            Reset
			
			        </button>
			
			
			        <button
			            type="button"
			            id="saveSettingsBtn"
			            class="settings-button primary">
			
			            <i class="fa-solid fa-floppy-disk"></i>
			
			            Save Changes
			
			        </button>
			
			    </div>
			
			</div>

        </div>

    </section>


    <!-- =====================================================
         FOOTER INFO
         ===================================================== -->

    <div class="settings-footer">

        <i class="fa-solid fa-shield-halved"></i>

        Configuration changes should be made through the
        application configuration files unless explicitly
        supported by this administration panel.

    </div>

</div>


<script>
    window.contextPath =
        "${pageContext.request.contextPath}";
</script>
<script
    src="${pageContext.request.contextPath}/js/settings.js">
</script>

<%@ include file="../layout/foot.jsp"%>
</body>

</html>