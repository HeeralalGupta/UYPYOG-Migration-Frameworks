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
</head>

<body>
	<!-- HEADER -->
	<%@ include file="../layout/header.jsp"%>

	<!-- NAVBAR -->
	<%@ include file="../layout/navbar.jsp"%>


<div class="reports-page">


    <!-- =====================================================
         PAGE HEADER
         ===================================================== -->

    <section class="reports-header">

        <div class="reports-header-content">

            <div class="reports-eyebrow">

                <i class="fa-solid fa-chart-line"></i>

                REPORTS & ANALYTICS

            </div>


            <h1>
                Migration Reports
            </h1>


            <p>
                Analyze migration performance, success rates,
                records processed and execution trends.
            </p>

        </div>


        <div class="reports-header-actions">

            <button
                type="button"
                id="refreshReportsBtn"
                class="reports-btn secondary">

                <i class="fa-solid fa-rotate"></i>

                Refresh

            </button>


            <button
                type="button"
                id="exportReportsBtn"
                class="reports-btn primary">

                <i class="fa-solid fa-file-excel"></i>

                Export

            </button>

        </div>

    </section>


    <!-- =====================================================
         FILTERS
         ===================================================== -->

    <section class="reports-panel filters-panel">


        <div class="reports-panel-header">


            <div class="reports-panel-title">

                <div class="reports-panel-icon blue">

                    <i class="fa-solid fa-filter"></i>

                </div>


                <div>

                    <h3>
                        Report Filters
                    </h3>

                    <p>
                        Narrow down analytics by period,
                        module and tenant.
                    </p>

                </div>

            </div>


            <button
                type="button"
                id="resetReportFiltersBtn"
                class="reset-report-btn">

                <i class="fa-solid fa-rotate-left"></i>

                Reset

            </button>


        </div>


        <div class="report-filter-grid">


            <!-- From Date -->

            <div class="report-filter-field">

                <label>
                    From Date
                </label>

                <input
                    type="date"
                    id="reportFromDate">

            </div>


            <!-- To Date -->

            <div class="report-filter-field">

                <label>
                    To Date
                </label>

                <input
                    type="date"
                    id="reportToDate">

            </div>


            <!-- Module -->

            <div class="report-filter-field">

                <label>
                    Module
                </label>

                <select id="reportModule">

                    <option value="ALL">
                        All Modules
                    </option>

                </select>

            </div>


            <!-- Tenant -->

            <div class="report-filter-field">

                <label>
                    Tenant
                </label>

                <select id="reportTenant">

                    <option value="ALL">
                        All Tenants
                    </option>

                </select>

            </div>


            <div class="report-filter-actions">

                <button
                    type="button"
                    id="generateReportBtn"
                    class="generate-report-btn">

                    <i class="fa-solid fa-chart-column"></i>

                    Generate Report

                </button>

            </div>


        </div>

    </section>


    <!-- =====================================================
         SUMMARY CARDS
         ===================================================== -->

    <section class="report-summary-grid">


        <div class="report-summary-card blue">

            <div class="report-summary-icon">

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


        <div class="report-summary-card green">

            <div class="report-summary-icon">

                <i class="fa-solid fa-circle-check"></i>

            </div>

            <div>

                <span>
                    Successful Jobs
                </span>

                <strong id="reportSuccessfulJobs">
                    0
                </strong>

            </div>

        </div>


        <div class="report-summary-card red">

            <div class="report-summary-icon">

                <i class="fa-solid fa-circle-xmark"></i>

            </div>

            <div>

                <span>
                    Failed / Errors
                </span>

                <strong id="reportFailedJobs">
                    0
                </strong>

            </div>

        </div>


        <div class="report-summary-card purple">

            <div class="report-summary-icon">

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


    </section>


    <!-- =====================================================
         SECONDARY KPIs
         ===================================================== -->

    <section class="report-kpi-grid">


        <div class="report-kpi-card">

            <span>
                Success Rate
            </span>

            <strong id="reportSuccessRate">
                0%
            </strong>

            <small>
                Successful jobs / total completed jobs
            </small>

        </div>


        <div class="report-kpi-card">

            <span>
                Running Jobs
            </span>

            <strong id="reportRunningJobs">
                0
            </strong>

            <small>
                Currently processing
            </small>

        </div>


        <div class="report-kpi-card">

            <span>
                Skipped Records
            </span>

            <strong id="reportSkippedRecords">
                0
            </strong>

            <small>
                Duplicate / skipped records
            </small>

        </div>


        <div class="report-kpi-card">

            <span>
                Average Duration
            </span>

            <strong id="reportAverageDuration">
                0s
            </strong>

            <small>
                Average completed job duration
            </small>

        </div>


    </section>


    <!-- =====================================================
         CHART ROW 1
         ===================================================== -->

    <section class="reports-chart-grid">


        <!-- Migration Trend -->

        <div class="reports-chart-panel large">

            <div class="reports-chart-header">

                <div class="reports-chart-title">

                    <div class="reports-chart-icon blue">

                        <i class="fa-solid fa-chart-line"></i>

                    </div>

                    <div>

                        <h3>
                            Migration Trend
                        </h3>

                        <p>
                            Jobs executed over the selected period.
                        </p>

                    </div>

                </div>

            </div>


            <div class="reports-chart-wrapper">

                <canvas
                    id="migrationTrendChart">
                </canvas>

            </div>

        </div>


        <!-- Success / Failed -->

        <div class="reports-chart-panel">

            <div class="reports-chart-header">

                <div class="reports-chart-title">

                    <div class="reports-chart-icon purple">

                        <i class="fa-solid fa-chart-pie"></i>

                    </div>

                    <div>

                        <h3>
                            Job Outcome
                        </h3>

                        <p>
                            Overall execution result.
                        </p>

                    </div>

                </div>

            </div>


            <div class="reports-doughnut-wrapper">

                <canvas
                    id="jobOutcomeChart">
                </canvas>

            </div>

        </div>

    </section>


    <!-- =====================================================
         CHART ROW 2
         ===================================================== -->

    <section class="reports-chart-grid">


        <!-- Module -->

        <div class="reports-chart-panel">

            <div class="reports-chart-header">

                <div class="reports-chart-title">

                    <div class="reports-chart-icon green">

                        <i class="fa-solid fa-layer-group"></i>

                    </div>

                    <div>

                        <h3>
                            Module Performance
                        </h3>

                        <p>
                            Jobs by migration module.
                        </p>

                    </div>

                </div>

            </div>


            <div class="reports-chart-wrapper">

                <canvas
                    id="modulePerformanceChart">
                </canvas>

            </div>

        </div>


        <!-- Tenant -->

        <div class="reports-chart-panel">

            <div class="reports-chart-header">

                <div class="reports-chart-title">

                    <div class="reports-chart-icon orange">

                        <i class="fa-solid fa-building"></i>

                    </div>

                    <div>

                        <h3>
                            Tenant Performance
                        </h3>

                        <p>
                            Migration jobs by tenant.
                        </p>

                    </div>

                </div>

            </div>


            <div class="reports-chart-wrapper">

                <canvas
                    id="tenantPerformanceChart">
                </canvas>

            </div>

        </div>

    </section>


    <!-- =====================================================
         RECORD SUMMARY
         ===================================================== -->

    <section class="reports-panel record-summary-panel">

        <div class="reports-panel-header">

            <div class="reports-panel-title">

                <div class="reports-panel-icon purple">

                    <i class="fa-solid fa-chart-simple"></i>

                </div>

                <div>

                    <h3>
                        Record Processing Summary
                    </h3>

                    <p>
                        Records processed across selected migrations.
                    </p>

                </div>

            </div>

        </div>


        <div class="record-summary-content">

            <div class="record-summary-item">

                <span>
                    Total
                </span>

                <strong id="summaryRecordsTotal">
                    0
                </strong>

            </div>


            <div class="record-summary-item success">

                <span>
                    Successful
                </span>

                <strong id="summaryRecordsSuccess">
                    0
                </strong>

            </div>


            <div class="record-summary-item failed">

                <span>
                    Failed
                </span>

                <strong id="summaryRecordsFailed">
                    0
                </strong>

            </div>


            <div class="record-summary-item skipped">

                <span>
                    Skipped
                </span>

                <strong id="summaryRecordsSkipped">
                    0
                </strong>

            </div>

        </div>

    </section>


</div>


<script>
    window.contextPath =
        "${pageContext.request.contextPath}";
</script>

<script
    src="https://cdn.jsdelivr.net/npm/chart.js">
</script>

<script
    src="${pageContext.request.contextPath}/js/reports.js">
</script>

	<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
	<script
		src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
	<!-- FOOTER -->
	<%@ include file="../layout/foot.jsp"%>
</body>

</html>