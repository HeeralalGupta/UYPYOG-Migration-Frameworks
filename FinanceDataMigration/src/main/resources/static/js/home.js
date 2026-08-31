/* ============================================================
 * FINANCE DATA MIGRATION
 * HOME DASHBOARD
 * ============================================================
 */


/* ============================================================
 * GLOBAL VARIABLES
 * ============================================================
 */

let dashboardRefreshTimer = null;

let migrationActivityChart = null;

const DASHBOARD_REFRESH_INTERVAL = 10000; // 10 seconds


/* ============================================================
 * PAGE INITIALIZATION
 * ============================================================
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        /*
         * Initial dashboard load
         */
        loadDashboard(true);


        /*
         * Initial migration activity chart
         */
        loadMigrationActivity(7);


        /*
         * Start automatic refresh
         */
        startDashboardAutoRefresh();


        /*
         * Activity range dropdown
         */
        initializeActivityRange();

    }
);


/* ============================================================
 * CONTEXT PATH
 * ============================================================
 */

function getContextPath() {

    return window.contextPath || "";

}


/* ============================================================
 * LOAD MAIN DASHBOARD
 * ============================================================
 */

async function loadDashboard(isInitialLoad = false) {

    try {

        /*
         * Show loading only during first page load.
         *
         * During auto refresh we keep existing data visible.
         */
        if (isInitialLoad) {

            showDashboardLoading();

        }


        const url =
            getContextPath()
            + "/migration/dashboard";


        console.log(
            "Dashboard API URL:",
            url
        );


        const response =
            await fetch(
                url,
                {
                    method: "GET",

                    headers: {
                        "Accept": "application/json"
                    },

                    cache: "no-store"
                }
            );


        console.log(
            "Dashboard API Status:",
            response.status
        );


        if (!response.ok) {

            throw new Error(
                "Dashboard API returned HTTP "
                + response.status
            );

        }


        const dashboard =
            await response.json();


        console.log(
            "Dashboard Data:",
            dashboard
        );


        /*
         * ====================================================
         * LAST UPDATED
         * ====================================================
         */

        updateElement(
            "dashboardLastUpdated",
            formatCurrentTime()
        );


        /*
         * ====================================================
         * MIGRATION MODULES
         * ====================================================
         */

        updateElement(
            "migrationModulesCount",
            dashboard.migrationModules ?? 0
        );


        updateElement(
            "migrationModulesLabel",
            dashboard.migrationModules ?? 0
        );


        /*
         * ====================================================
         * TOTAL JOBS
         * ====================================================
         */

        updateElement(
            "totalJobsCount",
            formatNumber(
                dashboard.totalJobs ?? 0
            )
        );


        /*
         * ====================================================
         * TODAY JOBS
         * ====================================================
         */

        updateElement(
            "todayJobsCount",
            "+"
            + formatNumber(
                dashboard.todayJobs ?? 0
            )
        );


        /*
         * ====================================================
         * SUCCESS RATE
         * ====================================================
         */

        const successRate =
            dashboard.successRate ?? 0;


        updateElement(
            "successRateCount",
            formatNumber(successRate)
            + "%"
        );


        updateSuccessRateMessage(
            successRate,

            dashboard.successfulJobs ?? 0,

            dashboard.failedJobs ?? 0
        );


        /*
         * ====================================================
         * RUNNING JOBS
         * ====================================================
         */

        const runningJobs =
            dashboard.runningJobs ?? 0;


        updateElement(
            "runningJobsCount",
            formatNumber(runningJobs)
        );


        updateRunningJobsMessage(
            runningJobs
        );


        /*
         * ====================================================
         * RECENT JOBS
         * ====================================================
         */

        renderRecentJobs(
            dashboard.recentJobs || []
        );


    } catch (error) {

        console.error(
            "Unable to load dashboard:",
            error
        );


        /*
         * Don't destroy the working dashboard during
         * an automatic refresh.
         */
        if (isInitialLoad) {

            showDashboardError();

        }

    }

}


/* ============================================================
 * CURRENT TIME
 * ============================================================
 */

function formatCurrentTime() {

    return new Date().toLocaleTimeString(
        "en-IN",
        {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        }
    );

}


/* ============================================================
 * SAFE ELEMENT UPDATE
 * ============================================================
 */

function updateElement(
    elementId,
    value
) {

    const element =
        document.getElementById(
            elementId
        );


    if (!element) {

        return;

    }


    element.textContent =
        value;

}


/* ============================================================
 * NUMBER FORMAT
 * ============================================================
 */

function formatNumber(value) {

    const number =
        Number(value);


    if (Number.isNaN(number)) {

        return "0";

    }


    return number.toLocaleString(
        "en-IN",
        {
            maximumFractionDigits: 1
        }
    );

}


/* ============================================================
 * SUCCESS RATE MESSAGE
 * ============================================================
 */

function updateSuccessRateMessage(
    rate,
    successfulJobs,
    failedJobs
) {

    const element =
        document.getElementById(
            "successRateMessage"
        );


    if (!element) {

        return;

    }


    if (
        successfulJobs === 0
        &&
        failedJobs === 0
    ) {

        element.textContent =
            "No completed jobs";

        return;

    }


    element.textContent =
        formatNumber(successfulJobs)
        + " Successful · "
        + formatNumber(failedJobs)
        + " Failed";

}


/* ============================================================
 * RUNNING JOB MESSAGE
 * ============================================================
 */

function updateRunningJobsMessage(
    runningJobs
) {

    const element =
        document.getElementById(
            "runningJobsMessage"
        );


    if (!element) {

        return;

    }


    if (runningJobs > 0) {

        element.innerHTML =
            '<i class="fa-solid fa-circle-notch fa-spin"></i> '
            + formatNumber(runningJobs)
            + ' Currently Processing';

    } else {

        element.innerHTML =
            '<i class="fa-solid fa-circle-check"></i> '
            + 'All processes idle';

    }

}


/* ============================================================
 * RENDER RECENT JOBS
 * ============================================================
 */

function renderRecentJobs(jobs) {

    const tbody =
        document.getElementById(
            "recentJobsTableBody"
        );


    if (!tbody) {

        return;

    }


    /*
     * No jobs
     */
    if (
        !jobs
        ||
        jobs.length === 0
    ) {

        tbody.innerHTML = `

            <tr>

                <td colspan="7">

                    <div class="empty-state">

                        <div class="empty-icon">

                            <i class="fa-solid fa-inbox"></i>

                        </div>


                        <strong>
                            No Migration Jobs
                        </strong>


                        <span>
                            No migration has been executed yet.
                        </span>


                        <a
                            href="${getContextPath()}/migration/new"
                            class="empty-action">

                            <i class="fa-solid fa-plus"></i>

                            Start Your First Migration

                        </a>

                    </div>

                </td>

            </tr>

        `;

        return;

    }


    /*
     * Render jobs
     */
    tbody.innerHTML =
        jobs.map(
            function (job) {

                const recordsText =
                    buildRecordSummary(job);


                return `

                    <tr>

                        <!-- JOB ID -->
                        <td>

                            <span class="job-id">

                                ${escapeHtml(
                                    shortenJobId(
                                        job.jobId
                                    )
                                )}

                            </span>

                        </td>


                        <!-- MODULE -->
                        <td>

                            <span class="module-name">

                                ${escapeHtml(
                                    formatModuleName(
                                        job.module
                                    )
                                )}

                            </span>

                        </td>


                        <!-- TENANT -->
                        <td>

                            ${escapeHtml(
                                job.tenant || "-"
                            )}

                        </td>


                        <!-- STATUS -->
                        <td>

                            ${getStatusBadge(
                                job.status
                            )}

                        </td>


                        <!-- STARTED -->
                        <td>

                            ${formatDateTime(
                                job.startedAt
                            )}

                        </td>


                        <!-- DURATION -->
                        <td>

                            <span
                                title="${escapeHtml(
                                    recordsText
                                )}">

                                ${calculateDuration(
                                    job.startedAt,
                                    job.completedAt
                                )}

                            </span>

                        </td>


                        <!-- ACTION -->
                        <td>

                            <a
                                href="${getContextPath()}/migration/history"
                                class="job-view-btn"
                                title="View Migration History">

                                <i
                                    class="fa-solid fa-arrow-right">
                                </i>

                            </a>

                        </td>

                    </tr>

                `;

            }
        ).join("");

}


/* ============================================================
 * RECORD SUMMARY
 * ============================================================
 */

function buildRecordSummary(job) {

    const total =
        job.totalRecords ?? 0;

    const success =
        job.successRecords ?? 0;

    const failed =
        job.failedRecords ?? 0;

    const skipped =
        job.skippedRecords ?? 0;


    return (
        "Total: " + total
        + " | Successful: " + success
        + " | Failed: " + failed
        + " | Skipped: " + skipped
    );

}


/* ============================================================
 * STATUS BADGE
 * ============================================================
 */

function getStatusBadge(status) {

    const normalized =
        (status || "")
            .toUpperCase();


    switch (normalized) {


        case "COMPLETED":

            return `

                <span class="status-badge success">

                    <i class="fa-solid fa-circle-check"></i>

                    Success

                </span>

            `;


        case "COMPLETED_WITH_ERRORS":

            return `

                <span class="status-badge failed">

                    <i class="fa-solid fa-circle-xmark"></i>

                    Completed with Errors

                </span>

            `;


        case "FAILED":

            return `

                <span class="status-badge failed">

                    <i class="fa-solid fa-circle-xmark"></i>

                    Failed

                </span>

            `;


        case "RUNNING":

            return `

                <span class="status-badge running">

                    <i class="fa-solid fa-spinner fa-spin"></i>

                    Running

                </span>

            `;


        case "PROCESSING":

            return `

                <span class="status-badge running">

                    <i class="fa-solid fa-spinner fa-spin"></i>

                    Processing

                </span>

            `;


        case "PENDING":

            return `

                <span class="status-badge pending">

                    <i class="fa-solid fa-clock"></i>

                    Pending

                </span>

            `;


        default:

            return `

                <span class="status-badge">

                    ${escapeHtml(
                        status || "-"
                    )}

                </span>

            `;

    }

}


/* ============================================================
 * MODULE NAME
 * ============================================================
 */

function formatModuleName(module) {

    if (!module) {

        return "-";

    }


    return module
        .toLowerCase()
        .split("_")
        .map(
            function (word) {

                return (
                    word.charAt(0).toUpperCase()
                    + word.slice(1)
                );

            }
        )
        .join(" ");

}


/* ============================================================
 * SHORT JOB ID
 * ============================================================
 */

function shortenJobId(jobId) {

    if (!jobId) {

        return "-";

    }


    if (jobId.length <= 12) {

        return jobId;

    }


    return (
        jobId.substring(0, 8)
        + "..."
        + jobId.substring(
            jobId.length - 4
        )
    );

}


/* ============================================================
 * DATE / TIME
 * ============================================================
 */

function formatDateTime(value) {

    if (!value) {

        return "-";

    }


    const date =
        new Date(value);


    if (
        Number.isNaN(
            date.getTime()
        )
    ) {

        return "-";

    }


    return date.toLocaleString(
        "en-IN",
        {
            day: "2-digit",
            month: "short",
            hour: "2-digit",
            minute: "2-digit"
        }
    );

}


/* ============================================================
 * DURATION
 * ============================================================
 */

function calculateDuration(
    startedAt,
    completedAt
) {

    if (!startedAt) {

        return "-";

    }


    const start =
        new Date(startedAt);


    const end =
        completedAt
            ? new Date(completedAt)
            : new Date();


    if (
        Number.isNaN(
            start.getTime()
        )
    ) {

        return "-";

    }


    const milliseconds =
        Math.max(
            0,
            end.getTime()
            - start.getTime()
        );


    const seconds =
        Math.floor(
            milliseconds / 1000
        );


    const hours =
        Math.floor(
            seconds / 3600
        );


    const minutes =
        Math.floor(
            (seconds % 3600) / 60
        );


    const remainingSeconds =
        seconds % 60;


    /*
     * HH MM SS
     */
    if (hours > 0) {

        return (
            hours + "h "
            + minutes + "m "
            + remainingSeconds + "s"
        );

    }


    /*
     * MM SS
     */
    if (minutes > 0) {

        return (
            minutes + "m "
            + remainingSeconds + "s"
        );

    }


    return remainingSeconds + "s";

}


/* ============================================================
 * ESCAPE HTML
 * ============================================================
 */

function escapeHtml(value) {

    if (value == null) {

        return "";

    }


    return String(value)
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );

}


/* ============================================================
 * LOAD MIGRATION ACTIVITY
 * ============================================================
 */

async function loadMigrationActivity(
    days = 7
) {

    try {

        const url =
            getContextPath()
            + "/migration/dashboard/activity?days="
            + encodeURIComponent(days);


        console.log(
            "Activity API URL:",
            url
        );


        const response =
            await fetch(
                url,
                {
                    method: "GET",

                    headers: {
                        "Accept": "application/json"
                    },

                    cache: "no-store"
                }
            );


        console.log(
            "Activity API Status:",
            response.status
        );


        if (!response.ok) {

            throw new Error(
                "Activity API returned HTTP "
                + response.status
            );

        }


        const data =
            await response.json();


        console.log(
            "Migration Activity:",
            data
        );


        renderMigrationActivityChart(
            data
        );


    } catch (error) {

        console.error(
            "Unable to load migration activity:",
            error
        );

    }

}


/* ============================================================
 * RENDER MIGRATION ACTIVITY CHART
 * ============================================================
 */

function renderMigrationActivityChart(
    data
) {

    console.log(
        "Rendering Migration Activity Chart"
    );


    const canvas =
        document.getElementById(
            "migrationActivityChart"
        );


    if (!canvas) {

        console.error(
            "Canvas #migrationActivityChart not found"
        );

        return;

    }


    if (
        typeof Chart === "undefined"
    ) {

        console.error(
            "Chart.js is not loaded"
        );

        return;

    }


    /*
     * Destroy previous chart
     */
    if (migrationActivityChart) {

        migrationActivityChart.destroy();

        migrationActivityChart = null;

    }


    /*
     * Safely prepare arrays
     */
    const labels =
        Array.isArray(data.labels)
            ? data.labels
            : [];


    const successful =
        Array.isArray(data.successful)
            ? data.successful
            : [];


    const failed =
        Array.isArray(data.failed)
            ? data.failed
            : [];


    const running =
        Array.isArray(data.running)
            ? data.running
            : [];


    /*
     * Create chart
     */
    migrationActivityChart =
        new Chart(
            canvas,
            {
                type: "line",

                data: {

                    labels: labels,

                    datasets: [

                        {
                            label: "Successful",

                            data: successful,

                            borderColor: "#2563eb",

                            backgroundColor:
                                "rgba(37, 99, 235, 0.10)",

                            borderWidth: 3,

                            pointRadius: 4,

                            pointHoverRadius: 7,

                            pointBorderWidth: 2,

                            tension: 0.35,

                            fill: true
                        },


                        {
                            label: "Failed",

                            data: failed,

                            borderColor: "#ef4444",

                            backgroundColor:
                                "rgba(239, 68, 68, 0.08)",

                            borderWidth: 3,

                            pointRadius: 4,

                            pointHoverRadius: 7,

                            pointBorderWidth: 2,

                            tension: 0.35,

                            fill: true
                        },


                        {
                            label: "Running",

                            data: running,

                            borderColor: "#8b5cf6",

                            backgroundColor:
                                "rgba(139, 92, 246, 0.08)",

                            borderWidth: 3,

                            pointRadius: 4,

                            pointHoverRadius: 7,

                            pointBorderWidth: 2,

                            borderDash: [6, 5],

                            tension: 0.35,

                            fill: false
                        }

                    ]

                },


                options: {

                    responsive: true,

                    maintainAspectRatio: false,


                    interaction: {

                        mode: "index",

                        intersect: false

                    },


                    animation: {

                        duration: 700

                    },


                    plugins: {

                        legend: {

                            display: true,

                            position: "top",

                            align: "end",

                            labels: {

                                usePointStyle: true,

                                pointStyle: "circle",

                                padding: 18

                            }

                        },


                        tooltip: {

                            enabled: true,

                            displayColors: true,

                            callbacks: {

                                title:
                                    function (
                                        tooltipItems
                                    ) {

                                        if (
                                            !tooltipItems
                                            ||
                                            tooltipItems.length === 0
                                        ) {

                                            return "";

                                        }


                                        return tooltipItems[0]
                                            .label;

                                    },


                                label:
                                    function (
                                        context
                                    ) {

                                        return (
                                            " "
                                            + context.dataset.label
                                            + ": "
                                            + context.parsed.y
                                        );

                                    }

                            }

                        }

                    },


                    scales: {

                        x: {

                            beginAtZero: true,

                            grid: {

                                display: false

                            },

                            ticks: {

                                maxRotation: 0,

                                autoSkip: true,

                                color: "#64748b"

                            }

                        },


                        y: {

                            beginAtZero: true,

                            suggestedMax: 5,

                            ticks: {

                                precision: 0,

                                stepSize: 1,

                                color: "#64748b"

                            },

                            grid: {

                                color:
                                    "rgba(148, 163, 184, 0.16)",

                                drawBorder: false

                            }

                        }

                    }

                }

            }
        );


    console.log(
        "Migration Activity Chart created successfully"
    );

}


/* ============================================================
 * ACTIVITY RANGE SELECTOR
 * ============================================================
 */

function initializeActivityRange() {

    const select =
        document.getElementById(
            "activityRange"
        );


    if (!select) {

        return;

    }


    select.addEventListener(
        "change",
        function () {

            const days =
                Number(this.value);


            loadMigrationActivity(
                days
            );

        }
    );

}


/* ============================================================
 * DASHBOARD AUTO REFRESH
 * ============================================================
 */

function startDashboardAutoRefresh() {

    /*
     * Clear existing timer
     */
    if (
        dashboardRefreshTimer !== null
    ) {

        clearInterval(
            dashboardRefreshTimer
        );

    }


    /*
     * Start new timer
     */
    dashboardRefreshTimer =
        setInterval(
            async function () {

                /*
                 * Refresh cards and recent jobs
                 */
                await loadDashboard(
                    false
                );


                /*
                 * Refresh graph
                 */
                const range =
                    document.getElementById(
                        "activityRange"
                    );


                const days =
                    range
                        ? Number(range.value)
                        : 7;


                await loadMigrationActivity(
                    days
                );

            },
            DASHBOARD_REFRESH_INTERVAL
        );

}


/* ============================================================
 * INITIAL LOADING STATE
 * ============================================================
 */

function showDashboardLoading() {

    updateElement(
        "migrationModulesCount",
        "..."
    );


    updateElement(
        "migrationModulesLabel",
        "..."
    );


    updateElement(
        "totalJobsCount",
        "..."
    );


    updateElement(
        "todayJobsCount",
        "..."
    );


    updateElement(
        "successRateCount",
        "..."
    );


    updateElement(
        "runningJobsCount",
        "..."
    );


    updateElement(
        "successRateMessage",
        "Loading..."
    );


    updateElement(
        "runningJobsMessage",
        "Loading..."
    );

}


/* ============================================================
 * DASHBOARD ERROR
 * ============================================================
 */

function showDashboardError() {

    updateElement(
        "migrationModulesCount",
        "-"
    );


    updateElement(
        "migrationModulesLabel",
        "-"
    );


    updateElement(
        "totalJobsCount",
        "-"
    );


    updateElement(
        "todayJobsCount",
        "-"
    );


    updateElement(
        "successRateCount",
        "-"
    );


    updateElement(
        "runningJobsCount",
        "-"
    );


    updateElement(
        "successRateMessage",
        "Unable to load statistics"
    );


    updateElement(
        "runningJobsMessage",
        "Dashboard unavailable"
    );


    const tbody =
        document.getElementById(
            "recentJobsTableBody"
        );


    if (!tbody) {

        return;

    }


    tbody.innerHTML = `

        <tr>

            <td colspan="7">

                <div class="empty-state">

                    <div class="empty-icon">

                        <i
                            class="fa-solid fa-triangle-exclamation">
                        </i>

                    </div>


                    <strong>
                        Dashboard Unavailable
                    </strong>


                    <span>
                        Unable to load migration statistics.
                    </span>


                    <button
                        type="button"
                        class="empty-action"
                        onclick="loadDashboard(true)">

                        <i
                            class="fa-solid fa-rotate-right">
                        </i>

                        Retry

                    </button>

                </div>

            </td>

        </tr>

    `;

}