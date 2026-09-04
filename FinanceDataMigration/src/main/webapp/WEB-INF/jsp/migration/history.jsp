<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Migration Upload</title>
<%@ include file="../layout/head.jsp"%>
<link
	href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css"
	rel="stylesheet" />
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/history.css">
</head>

<body>
	<!-- HEADER -->
	<%@ include file="../layout/header.jsp"%>

	<!-- NAVBAR -->
	<%@ include file="../layout/navbar.jsp"%>

<div class="container migration-content-width history-page">


    <!-- =====================================================
         PAGE HEADER
         ===================================================== -->

    <div class="history-page-header">

        <div>

            <div class="history-eyebrow">

                <i class="fa-solid fa-chart-column"></i>

                REPORTS &amp; ANALYTICS

            </div>


            <h1>
                Migration History
            </h1>


            <p>
                Monitor, filter and analyze all migration executions.
            </p>

        </div>


        <div class="history-header-actions">

            <button
                type="button"
                id="refreshHistoryBtn"
                class="history-btn secondary">

                <i class="fa-solid fa-rotate"></i>

                Refresh

            </button>


            <button
                type="button"
                id="exportHistoryBtn"
                class="history-btn primary">

                <i class="fa-solid fa-file-export"></i>

                Export

            </button>

        </div>

    </div>


    <!-- =====================================================
         SUMMARY CARDS
         ===================================================== -->

    <div class="row g-4 mt-2">


        <div class="col-xl-3 col-lg-6">

            <div class="report-stat-card">

                <div class="report-stat-icon blue">

                    <i class="fa-solid fa-database"></i>

                </div>

                <div>

                    <span>
                        Total Jobs
                    </span>

                    <strong id="reportTotalJobs">
                        0
                    </strong>

                </div>

            </div>

        </div>


        <div class="col-xl-3 col-lg-6">

            <div class="report-stat-card">

                <div class="report-stat-icon green">

                    <i class="fa-solid fa-circle-check"></i>

                </div>

                <div>

                    <span>
                        Successful
                    </span>

                    <strong id="reportSuccessfulJobs">
                        0
                    </strong>

                </div>

            </div>

        </div>


        <div class="col-xl-3 col-lg-6">

            <div class="report-stat-card">

                <div class="report-stat-icon red">

                    <i class="fa-solid fa-circle-xmark"></i>

                </div>

                <div>

                    <span>
                        Failed
                    </span>

                    <strong id="reportFailedJobs">
                        0
                    </strong>

                </div>

            </div>

        </div>


        <div class="col-xl-3 col-lg-6">

            <div class="report-stat-card">

                <div class="report-stat-icon purple">

                    <i class="fa-solid fa-list-check"></i>

                </div>

                <div>

                    <span>
                        Records Processed
                    </span>

                    <strong id="reportTotalRecords">
                        0
                    </strong>

                </div>

            </div>

        </div>

    </div>
    
    <!-- =====================================================
         FILTER PANEL
         ===================================================== -->

    <div class="dashboard-panel filters-panel mt-4">


        <div class="panel-header">

            <div class="panel-title">

                <div class="panel-icon blue-icon">

                    <i class="fa-solid fa-filter"></i>

                </div>


                <div>

                    <h4>
                        Filters
                    </h4>

                    <p>
                        Narrow down migration history
                    </p>

                </div>

            </div>


            <button
                type="button"
                id="resetFiltersBtn"
                class="filter-reset-btn">

                <i class="fa-solid fa-rotate-left"></i>

                Reset

            </button>

        </div>


        <div class="filter-grid">


            <div class="filter-field">

                <label>
                    Job ID
                </label>

                <div class="filter-input-wrapper">

                    <i class="fa-solid fa-hashtag"></i>

                    <input
                        type="text"
                        id="filterJobId"
                        placeholder="Search job ID">

                </div>

            </div>


            <div class="filter-field">

                <label>
                    Module
                </label>

                <select id="filterModule">

                    <option value="ALL">
                        All Modules
                    </option>

                </select>

            </div>


            <div class="filter-field">

                <label>
                    Status
                </label>

                <select id="filterStatus">

                    <option value="ALL">
                        All Status
                    </option>

                    <option value="COMPLETED">
                        Success
                    </option>

                    <option value="COMPLETED_WITH_ERRORS">
                        Completed with Errors
                    </option>

                    <option value="FAILED">
                        Failed
                    </option>

                    <option value="RUNNING">
                        Running
                    </option>

                    <option value="PROCESSING">
                        Processing
                    </option>

                </select>

            </div>


            <div class="filter-field">

			    <label>
			        Tenant
			    </label>
			
			    <select id="filterTenant" data-tenant-dropdown>
			
			       <option value="">
				        Loading...
				    </option>
			
			    </select>
			
			</div>


            <div class="filter-field">

                <label>
                    From Date
                </label>

                <input
                    type="date"
                    id="filterFromDate">

            </div>


            <div class="filter-field">

                <label>
                    To Date
                </label>

                <input
                    type="date"
                    id="filterToDate">

            </div>


            <div class="filter-actions">

                <button
                    type="button"
                    id="searchHistoryBtn"
                    class="search-history-btn">

                    <i class="fa-solid fa-magnifying-glass"></i>

                    Search

                </button>

            </div>

        </div>

    </div>
    
    
        <!-- =====================================================
         JOB TABLE
         ===================================================== -->

    <div class="dashboard-panel history-table-panel mt-4">


        <div class="panel-header">

            <div class="panel-title">

                <div class="panel-icon green-icon">

                    <i class="fa-solid fa-clock-rotate-left"></i>

                </div>


                <div>

                    <h4>
                        Migration Jobs
                    </h4>

                    <p id="historyResultLabel">
                        Loading migration history...
                    </p>

                </div>

            </div>


            <div class="table-page-size">

                <label>
                    Show
                </label>

                <select id="pageSize">

                    <option value="10">
                        10
                    </option>

                    <option value="20">
                        20
                    </option>

                    <option value="50">
                        50
                    </option>

                </select>

            </div>

        </div>


        <div class="table-wrapper">

            <table class="history-table">


                <thead>

                    <tr>

                        <th>
                            Job ID
                        </th>

                        <th>
                            Module
                        </th>

                        <th>
                            Tenant
                        </th>

                        <th>
                            Status
                        </th>

                        <th>
                            Records
                        </th>

                        <th>
                            Started
                        </th>

                        <th>
                            Duration
                        </th>

                        <th>
                            Progress
                        </th>

                    </tr>

                </thead>


                <tbody id="historyTableBody">

                    <tr>

                        <td colspan="8">

                            <div class="history-loading">

                                <i
                                    class="fa-solid fa-spinner fa-spin">
                                </i>

                                Loading migration history...

                            </div>

                        </td>

                    </tr>

                </tbody>


            </table>

        </div>


        <!-- =================================================
             PAGINATION
             ================================================= -->

        <div class="history-pagination">

            <div
                id="paginationSummary"
                class="pagination-summary">

                Showing 0 jobs

            </div>


            <div
                id="paginationControls"
                class="pagination-controls">
            </div>

        </div>

    </div>


    

    <!-- =====================================================
         ACTIVITY CHART
         ===================================================== -->

    <div class="dashboard-panel report-chart-panel mt-4">


        <div class="panel-header">

            <div class="panel-title">

                <div class="panel-icon purple-icon">

                    <i class="fa-solid fa-chart-line"></i>

                </div>


                <div>

                    <h4>
                        Migration Activity
                    </h4>

                    <p>
                        Filtered migration execution overview
                    </p>

                </div>

            </div>


            <div class="chart-period-label">

                <i class="fa-regular fa-calendar"></i>

                Selected period

            </div>

        </div>


        <div class="report-chart-wrapper">

            <canvas id="historyActivityChart"></canvas>

        </div>

    </div>





</div>
	
	<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
	<script
    src="https://cdn.jsdelivr.net/npm/tom-select@2.4.3/dist/js/tom-select.complete.min.js">
</script>
	<!-- FOOTER -->
	<%@ include file="../layout/foot.jsp"%>
	<script src="${pageContext.request.contextPath}/js/history.js"></script>
</body>

</html>