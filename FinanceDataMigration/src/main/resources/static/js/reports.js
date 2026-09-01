"use strict";


let migrationTrendChart = null;
let jobOutcomeChart = null;
let modulePerformanceChart = null;
let tenantPerformanceChart = null;


document.addEventListener(
    "DOMContentLoaded",
    function() {

        initializeReports();

    }
);


/* ============================================================
 * INITIALIZE
 * ============================================================
 */

function initializeReports() {

    loadReportOptions();

    initializeEvents();

    generateReport();

}


/* ============================================================
 * EVENTS
 * ============================================================
 */

function initializeEvents() {

    const generateButton =
        document.getElementById(
            "generateReportBtn"
        );

    if (generateButton) {

        generateButton.addEventListener(
            "click",
            generateReport
        );

    }


    const refreshButton =
        document.getElementById(
            "refreshReportsBtn"
        );

    if (refreshButton) {

        refreshButton.addEventListener(
            "click",
            generateReport
        );

    }


    const resetButton =
        document.getElementById(
            "resetReportFiltersBtn"
        );

    if (resetButton) {

        resetButton.addEventListener(
            "click",
            resetReportFilters
        );

    }


    const exportButton =
        document.getElementById(
            "exportReportsBtn"
        );

    if (exportButton) {

        exportButton.addEventListener(
            "click",
            exportReport
        );

    }

}


/* ============================================================
 * OPTIONS
 * ============================================================
 */

async function loadReportOptions() {

    try {

        const response =
            await fetch(
                getContextPath()
                +
                "/migration/history/options",
                {
                    headers: {
                        "Accept":
                            "application/json"
                    },

                    cache: "no-store"
                }
            );


        if (!response.ok) {

            throw new Error(
                "Options API returned HTTP "
                +
                response.status
            );

        }


        const data =
            await response.json();


        populateSelect(
            "reportModule",
            data.modules || [],
            "All Modules"
        );


        populateSelect(
            "reportTenant",
            data.tenants || [],
            "All Tenants"
        );

    }
    catch (error) {

        console.error(
            "Unable to load report options:",
            error
        );

    }

}


/* ============================================================
 * POPULATE SELECT
 * ============================================================
 */

function populateSelect(
    elementId,
    values,
    allText
) {

    const select =
        document.getElementById(
            elementId
        );


    if (!select) {

        return;

    }


    select.innerHTML =
        `
        <option value="ALL">
            ${allText}
        </option>
        `;


    values.forEach(
        function(value) {

            if (!value) {

                return;

            }


            const option =
                document.createElement(
                    "option"
                );


            option.value =
                value;


            option.textContent =
                elementId === "reportModule"
                    ? formatModule(value)
                    : formatTenant(value);


            select.appendChild(
                option
            );

        }
    );

}


/* ============================================================
 * GENERATE REPORT
 * ============================================================
 */

async function generateReport() {

    try {

        const params =
            buildReportParams();


        const url =
            getContextPath()
            +
            "/migration/reports/data"
            +
            (
                params.toString()
                    ? "?"
                    +
                    params.toString()
                    : ""
            );


        console.log(
            "Report API:",
            url
        );


        const response =
            await fetch(
                url,
                {
                    method: "GET",

                    headers: {
                        "Accept":
                            "application/json"
                    },

                    cache: "no-store"
                }
            );


        if (!response.ok) {

            throw new Error(
                "Reports API returned HTTP "
                +
                response.status
            );

        }


        const data =
            await response.json();


        console.log(
            "Report Data:",
            data
        );


        renderSummary(
            data
        );


        renderTrendChart(
            data
        );


        renderOutcomeChart(
            data
        );


        renderModuleChart(
            data
        );


        renderTenantChart(
            data
        );


    }
    catch (error) {

        console.error(
            "Unable to generate report:",
            error
        );

    }

}


/* ============================================================
 * PARAMS
 * ============================================================
 */

function buildReportParams() {

    const params =
        new URLSearchParams();


    const fromDate =
        document.getElementById(
            "reportFromDate"
        )?.value;


    const toDate =
        document.getElementById(
            "reportToDate"
        )?.value;


    const module =
        document.getElementById(
            "reportModule"
        )?.value;


    const tenant =
        document.getElementById(
            "reportTenant"
        )?.value;


    if (fromDate) {

        params.set(
            "fromDate",
            fromDate
        );

    }


    if (toDate) {

        params.set(
            "toDate",
            toDate
        );

    }


    if (
        module &&
        module !== "ALL"
    ) {

        params.set(
            "module",
            module
        );

    }


    if (
        tenant &&
        tenant !== "ALL"
    ) {

        params.set(
            "tenant",
            tenant
        );

    }


    return params;

}


/* ============================================================
 * SUMMARY
 * ============================================================
 */

function renderSummary(data) {

    setText(
        "reportTotalJobs",
        formatNumber(
            data.totalJobs
        )
    );


    setText(
        "reportSuccessfulJobs",
        formatNumber(
            data.successfulJobs
        )
    );


    setText(
        "reportFailedJobs",
        formatNumber(
            data.failedJobs
        )
    );


    setText(
        "reportTotalRecords",
        formatNumber(
            data.totalRecords
        )
    );


    setText(
        "reportSuccessRate",
        Number(
            data.successRate || 0
        ).toFixed(1)
        +
        "%"
    );


    setText(
        "reportRunningJobs",
        formatNumber(
            data.runningJobs
        )
    );


    setText(
        "reportSkippedRecords",
        formatNumber(
            data.skippedRecords
        )
    );


    setText(
        "reportAverageDuration",
        formatDuration(
            data.averageDurationSeconds
        )
    );


    setText(
        "summaryRecordsTotal",
        formatNumber(
            data.totalRecords
        )
    );


    setText(
        "summaryRecordsSuccess",
        formatNumber(
            data.totalRecords -
            (
                data.failedRecords || 0
            ) -
            (
                data.skippedRecords || 0
            )
        )
    );


    setText(
        "summaryRecordsFailed",
        formatNumber(
            data.failedRecords || 0
        )
    );


    setText(
        "summaryRecordsSkipped",
        formatNumber(
            data.skippedRecords || 0
        )
    );

}


/* ============================================================
 * TREND CHART
 * ============================================================
 */

function renderTrendChart(data) {

    const canvas =
        document.getElementById(
            "migrationTrendChart"
        );


    if (!canvas || typeof Chart === "undefined") {

        return;

    }


    if (migrationTrendChart) {

        migrationTrendChart.destroy();

    }


    migrationTrendChart =
        new Chart(
            canvas,
            {
                type: "line",

                data: {

                    labels:
                        data.trendLabels || [],

                    datasets: [

                        {
                            label:
                                "Successful",

                            data:
                                data.trendSuccessful || [],

                            borderColor:
                                "#22c55e",

                            backgroundColor:
                                "rgba(34,197,94,0.10)",

                            fill: true,

                            tension: 0.35,

                            borderWidth: 2,

                            pointRadius: 3
                        },


                        {
                            label:
                                "Failed / Errors",

                            data:
                                data.trendFailed || [],

                            borderColor:
                                "#ef4444",

                            backgroundColor:
                                "rgba(239,68,68,0.08)",

                            fill: true,

                            tension: 0.35,

                            borderWidth: 2,

                            pointRadius: 3
                        },


                        {
                            label:
                                "Running",

                            data:
                                data.trendRunning || [],

                            borderColor:
                                "#8b5cf6",

                            backgroundColor:
                                "rgba(139,92,246,0.07)",

                            fill: false,

                            tension: 0.35,

                            borderWidth: 2,

                            pointRadius: 3
                        }

                    ]

                },

                options: chartOptions()

            }
        );

}


/* ============================================================
 * OUTCOME CHART
 * ============================================================
 */

function renderOutcomeChart(data) {

    const canvas =
        document.getElementById(
            "jobOutcomeChart"
        );


    if (!canvas || typeof Chart === "undefined") {

        return;

    }


    if (jobOutcomeChart) {

        jobOutcomeChart.destroy();

    }


    jobOutcomeChart =
        new Chart(
            canvas,
            {
                type: "doughnut",

                data: {

                    labels: [
                        "Successful",
                        "Failed / Errors",
                        "Running"
                    ],

                    datasets: [
                        {
                            data: [
                                data.successfulJobs || 0,
                                data.failedJobs || 0,
                                data.runningJobs || 0
                            ],

                            backgroundColor: [
                                "#22c55e",
                                "#ef4444",
                                "#8b5cf6"
                            ],

                            borderWidth: 0
                        }
                    ]

                },

                options: {

                    responsive: true,

                    maintainAspectRatio: false,

                    cutout: "68%",

                    plugins: {

                        legend: {

                            position: "bottom",

                            labels: {
                                usePointStyle: true,
                                padding: 14
                            }

                        }

                    }

                }

            }
        );

}


/* ============================================================
 * MODULE CHART
 * ============================================================
 */

function renderModuleChart(data) {

    const canvas =
        document.getElementById(
            "modulePerformanceChart"
        );


    if (!canvas || typeof Chart === "undefined") {

        return;

    }


    if (modulePerformanceChart) {

        modulePerformanceChart.destroy();

    }


    const entries =
        Object.entries(
            data.moduleJobs || {}
        );


    modulePerformanceChart =
        new Chart(
            canvas,
            {
                type: "bar",

                data: {

                    labels:
                        entries.map(
                            entry =>
                                formatModule(
                                    entry[0]
                                )
                        ),

                    datasets: [

                        {
                            label:
                                "Jobs",

                            data:
                                entries.map(
                                    entry =>
                                        entry[1]
                                ),

                            backgroundColor:
                                "rgba(59,130,246,0.72)",

                            borderRadius:
                                7
                        }

                    ]

                },

                options: {

                    responsive: true,

                    maintainAspectRatio: false,

                    plugins: {

                        legend: {
                            display: false
                        }

                    },

                    scales: {

                        y: {
                            beginAtZero: true,

                            ticks: {
                                precision: 0
                            }

                        }

                    }

                }

            }
        );

}


/* ============================================================
 * TENANT CHART
 * ============================================================
 */

function renderTenantChart(data) {

    const canvas =
        document.getElementById(
            "tenantPerformanceChart"
        );


    if (!canvas || typeof Chart === "undefined") {

        return;

    }


    if (tenantPerformanceChart) {

        tenantPerformanceChart.destroy();

    }


    const entries =
        Object.entries(
            data.tenantJobs || {}
        );


    tenantPerformanceChart =
        new Chart(
            canvas,
            {
                type: "bar",

                data: {

                    labels:
                        entries.map(
                            entry =>
                                formatTenant(
                                    entry[0]
                                )
                        ),

                    datasets: [

                        {
                            label:
                                "Jobs",

                            data:
                                entries.map(
                                    entry =>
                                        entry[1]
                                ),

                            backgroundColor:
                                "rgba(245,158,11,0.72)",

                            borderRadius:
                                7
                        }

                    ]

                },

                options: {

                    indexAxis:
                        "y",

                    responsive: true,

                    maintainAspectRatio: false,

                    plugins: {

                        legend: {
                            display: false
                        }

                    },

                    scales: {

                        x: {
                            beginAtZero: true,

                            ticks: {
                                precision: 0
                            }

                        }

                    }

                }

            }
        );

}


/* ============================================================
 * RESET
 * ============================================================
 */

function resetReportFilters() {

    const fromDate =
        document.getElementById(
            "reportFromDate"
        );

    const toDate =
        document.getElementById(
            "reportToDate"
        );

    const module =
        document.getElementById(
            "reportModule"
        );

    const tenant =
        document.getElementById(
            "reportTenant"
        );


    if (fromDate) {
        fromDate.value = "";
    }

    if (toDate) {
        toDate.value = "";
    }

    if (module) {
        module.value = "ALL";
    }

    if (tenant) {
        tenant.value = "ALL";
    }


    generateReport();

}


/* ============================================================
 * EXPORT
 * ============================================================
 */

function exportReport() {

    const params =
        buildReportParams();


    const query =
        params.toString();


    const url =
        getContextPath()
        +
        "/migration/reports/export"
        +
        (
            query
                ? "?" + query
                : ""
        );


    console.log(
        "Export Report:",
        url
    );


    window.location.href =
        url;
}


/* ============================================================
 * HELPERS
 * ============================================================
 */

function chartOptions() {

    return {

        responsive: true,

        maintainAspectRatio: false,

        interaction: {

            mode: "index",

            intersect: false

        },

        plugins: {

            legend: {

                position: "top",

                align: "end",

                labels: {

                    usePointStyle: true,

                    padding: 16

                }

            }

        },

        scales: {

            x: {

                grid: {
                    display: false
                }

            },

            y: {

                beginAtZero: true,

                ticks: {
                    precision: 0
                },

                grid: {

                    color:
                        "rgba(148,163,184,0.14)"

                }

            }

        }

    };

}


function formatModule(
    module
) {

    if (!module) {
        return "-";
    }

    return String(module)
        .toLowerCase()
        .split("_")
        .map(
            word =>
                word.charAt(0).toUpperCase()
                +
                word.slice(1)
        )
        .join(" ");

}


function formatTenant(
    tenant
) {

    if (!tenant) {
        return "";
    }

    const value =
        String(tenant)
            .trim();


    const parts =
        value.split(".");


    const name =
        parts.length > 1
            ? parts.slice(1).join(".")
            : parts[0];


    return (
        name.charAt(0).toUpperCase()
        +
        name.slice(1).toLowerCase()
    );

}


function formatNumber(
    value
) {

    return Number(
        value || 0
    ).toLocaleString(
        "en-IN"
    );

}


function formatDuration(
    seconds
) {

    const value =
        Number(
            seconds || 0
        );


    if (value < 60) {

        return Math.round(value) + "s";

    }


    const minutes =
        Math.floor(
            value / 60
        );


    const remaining =
        Math.round(
            value % 60
        );


    return (
        minutes
        +
        "m "
        +
        remaining
        +
        "s"
    );

}


function setText(
    elementId,
    value
) {

    const element =
        document.getElementById(
            elementId
        );


    if (element) {

        element.textContent =
            value;

    }

}


function getContextPath() {

    return window.contextPath || "";

}

