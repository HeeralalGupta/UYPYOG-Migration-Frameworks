/* ============================================================
 * FINANCE DATA MIGRATION
 * MIGRATION HISTORY / REPORTS
 * ============================================================
 */

"use strict";


/* ============================================================
 * GLOBAL VARIABLES
 * ============================================================
 */

let historyChart = null;

let currentPage = 0;

let currentPageSize = 10;

let moduleSelect = null;

let statusSelect = null;

let tenantSelect = null;


/* ============================================================
 * INITIALIZATION
 * ============================================================
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        /*
         * IMPORTANT:
         * This class scopes all History-page-specific
         * Tom Select CSS and prevents affecting other pages.
         */
        document.body.classList.add(
            "history-page-active"
        );

        initializeHistory();

    }
);


/* ============================================================
 * INITIALIZE HISTORY
 * ============================================================
 */

function initializeHistory() {

    initializeSearchableDropdowns();

    initializeEvents();

    loadFilterOptions();

    loadHistory();

}


/* ============================================================
 * EVENTS
 * ============================================================
 */

function initializeEvents() {

    const searchButton =
        document.getElementById(
            "searchHistoryBtn"
        );

    if (searchButton) {

        searchButton.addEventListener(
            "click",
            function () {

                currentPage = 0;

                loadHistory();

            }
        );

    }


    const resetButton =
        document.getElementById(
            "resetFiltersBtn"
        );

    if (resetButton) {

        resetButton.addEventListener(
            "click",
            resetFilters
        );

    }


    const refreshButton =
        document.getElementById(
            "refreshHistoryBtn"
        );

    if (refreshButton) {

        refreshButton.addEventListener(
            "click",
            function () {

                loadHistory();

            }
        );

    }


    const pageSize =
        document.getElementById(
            "pageSize"
        );

    if (pageSize) {

        currentPageSize =
            Number(
                pageSize.value
            ) || 10;


        pageSize.addEventListener(
            "change",
            function () {

                currentPageSize =
                    Number(
                        this.value
                    ) || 10;

                currentPage = 0;

                loadHistory();

            }
        );

    }


    const jobIdInput =
        document.getElementById(
            "filterJobId"
        );

    if (jobIdInput) {

        jobIdInput.addEventListener(
            "keydown",
            function (event) {

                if (
                    event.key === "Enter"
                ) {

                    event.preventDefault();

                    currentPage = 0;

                    loadHistory();

                }

            }
        );

    }


    const exportButton =
        document.getElementById(
            "exportHistoryBtn"
        );

    if (exportButton) {

        exportButton.addEventListener(
            "click",
            exportHistory
        );

    }

}


/* ============================================================
 * SEARCHABLE DROPDOWNS
 * ============================================================
 */

function initializeSearchableDropdowns() {

    if (
        typeof TomSelect ===
        "undefined"
    ) {

        console.error(
            "Tom Select is not loaded."
        );

        return;

    }


    /*
     * MODULE
     */

    const moduleElement =
        document.getElementById(
            "filterModule"
        );


    if (
        moduleElement &&
        !moduleElement.tomselect
    ) {

        moduleSelect =
            new TomSelect(
                moduleElement,
                {

                    create: false,

                    allowEmptyOption: false,

                    maxOptions: 1000,

                    searchField: [
                        "text"
                    ],

                    placeholder:
                        "Search modules...",

                    closeAfterSelect:
                        true,

                    openOnFocus:
                        true,

                    dropdownParent:
                        "body"

                }
            );

    }


    /*
     * STATUS
     */

    const statusElement =
        document.getElementById(
            "filterStatus"
        );


    if (
        statusElement &&
        !statusElement.tomselect
    ) {

        statusSelect =
            new TomSelect(
                statusElement,
                {

                    create: false,

                    allowEmptyOption: false,

                    maxOptions: 100,

                    searchField: [
                        "text"
                    ],

                    placeholder:
                        "Search status...",

                    closeAfterSelect:
                        true,

                    openOnFocus:
                        true,

                    dropdownParent:
                        "body"

                }
            );

    }


    /*
     * TENANT
     */

    const tenantElement =
        document.getElementById(
            "filterTenant"
        );


    if (
        tenantElement &&
        !tenantElement.tomselect
    ) {

        tenantSelect =
            new TomSelect(
                tenantElement,
                {

                    create: false,

                    allowEmptyOption: false,

                    maxOptions: 1000,

                    searchField: [
                        "text"
                    ],

                    placeholder:
                        "Search tenants...",

                    closeAfterSelect:
                        true,

                    openOnFocus:
                        true,

                    dropdownParent:
                        "body"

                }
            );

    }

}


/* ============================================================
 * LOAD FILTER OPTIONS
 * ============================================================
 */

async function loadFilterOptions() {

    try {

        const response =
            await fetch(
                getContextPath()
                +
                "/migration/history/options",
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
                "Options API returned HTTP "
                +
                response.status
            );

        }


        const data =
            await response.json();


        console.log(
            "History Filter Options:",
            data
        );


        populateModuleDropdown(
            data.modules || []
        );


        populateTenantDropdown(
            data.tenants || []
        );


    } catch (error) {

        console.error(
            "Unable to load history filter options:",
            error
        );

    }

}


/* ============================================================
 * POPULATE MODULE
 * ============================================================
 */

function populateModuleDropdown(
    modules
) {

    if (!moduleSelect) {

        return;

    }


    moduleSelect.clearOptions();


    moduleSelect.addOption(
        {
            value: "ALL",
            text: "All Modules"
        }
    );


    modules.forEach(
        function (module) {

            if (
                module === null ||
                module === undefined ||
                String(module).trim() === ""
            ) {

                return;

            }


            moduleSelect.addOption(
                {
                    value: String(module),
                    text:
                        formatModuleName(
                            String(module)
                        )
                }
            );

        }
    );


    moduleSelect.setValue(
        "ALL",
        true
    );

}


/* ============================================================
 * POPULATE TENANT
 * ============================================================
 */

function populateTenantDropdown(tenants) {

    if (!tenantSelect) {
        return;
    }

    tenantSelect.clearOptions();

    tenantSelect.addOption({
        value: "ALL",
        text: "All Tenants"
    });

    tenants.forEach(function (tenant) {

        if (
            tenant === null ||
            tenant === undefined ||
            String(tenant).trim() === ""
        ) {
            return;
        }

        const value = String(tenant).trim();

        /*
         * hr.gurugram -> Gurugram
         * hr.ambala -> Ambala
         * hr.faridabad -> Faridabad
         */
        const displayName =
            getTenantDisplayName(value);

        tenantSelect.addOption({
            value: value,
            text: displayName
        });

    });

    tenantSelect.setValue("ALL", true);
}

function getTenantDisplayName(tenant) {

    if (!tenant) {
        return "";
    }

    const value = String(tenant).trim();

    /*
     * Take everything after the first dot.
     *
     * hr.gurugram
     *       ↓
     * gurugram
     */
    const parts = value.split(".");

    let name =
        parts.length > 1
            ? parts.slice(1).join(".")
            : parts[0];

    /*
     * First letter uppercase,
     * rest lowercase.
     */
    return (
        name.charAt(0).toUpperCase()
        +
        name.slice(1).toLowerCase()
    );
}


/* ============================================================
 * LOAD HISTORY
 * ============================================================
 */

async function loadHistory() {

    showTableLoading();


    try {

        const params =
            buildFilterParams();


        params.set(
            "page",
            currentPage
        );


        params.set(
            "pageSize",
            currentPageSize
        );


        const url =
            getContextPath()
            +
            "/migration/history/data?"
            +
            params.toString();


        console.log(
            "History API:",
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
                "History API returned HTTP "
                +
                response.status
            );

        }


        const data =
            await response.json();


        console.log(
            "History API Data:",
            data
        );


        renderSummary(
            data
        );


        renderHistoryTable(
            data
        );


        renderPagination(
            data
        );


        renderActivityChart(
            Array.isArray(data.jobs)
                ? data.jobs
                : []
        );


        updateResultLabel(
            data
        );


    } catch (error) {

        console.error(
            "Unable to load migration history:",
            error
        );


        showTableError();

    }

}


/* ============================================================
 * FILTER PARAMETERS
 * ============================================================
 */

function buildFilterParams() {

    const params =
        new URLSearchParams();


    const jobId =
        getInputValue(
            "filterJobId"
        );


    const module =
        getSelectValue(
            moduleSelect,
            "filterModule"
        );


    const status =
        getSelectValue(
            statusSelect,
            "filterStatus"
        );


    const tenant =
        getSelectValue(
            tenantSelect,
            "filterTenant"
        );


    const fromDate =
        getInputValue(
            "filterFromDate"
        );


    const toDate =
        getInputValue(
            "filterToDate"
        );


    if (jobId) {

        params.set(
            "jobId",
            jobId
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
        status &&
        status !== "ALL"
    ) {

        params.set(
            "status",
            status
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


    return params;

}


/* ============================================================
 * GET CONTEXT PATH
 * ============================================================
 */

function getContextPath() {

    if (
        window.contextPath !==
        undefined
    ) {

        return window.contextPath;

    }


    const path =
        window.location.pathname;


    const segments =
        path.split("/");


    if (
        segments.length > 1 &&
        segments[1]
    ) {

        return "/" + segments[1];

    }


    return "";

}


/* ============================================================
 * GET NATIVE INPUT VALUE
 * ============================================================
 */

function getInputValue(
    elementId
) {

    const element =
        document.getElementById(
            elementId
        );


    if (!element) {

        return "";

    }


    return String(
        element.value || ""
    ).trim();

}


/* ============================================================
 * GET TOM SELECT VALUE
 * ============================================================
 */

function getSelectValue(
    selectInstance,
    elementId
) {

    if (selectInstance) {

        return String(
            selectInstance.getValue() || ""
        ).trim();

    }


    return getInputValue(
        elementId
    );

}


/* ============================================================
 * SUMMARY
 * ============================================================
 */

function renderSummary(
    data
) {

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

}


/* ============================================================
 * TABLE
 * ============================================================
 */

function renderHistoryTable(
    data
) {

    const tbody =
        document.getElementById(
            "historyTableBody"
        );


    if (!tbody) {

        return;

    }


    const jobs =
        Array.isArray(data.jobs)
            ? data.jobs
            : [];


    if (
        jobs.length === 0
    ) {

        tbody.innerHTML = `

            <tr>

                <td colspan="8">

                    <div class="history-empty">

                        <div class="history-empty-icon">

                            <i class="fa-solid fa-magnifying-glass"></i>

                        </div>

                        <strong>
                            No migration jobs found
                        </strong>

                        <span>
                            No jobs match the selected filters.
                            Try changing your search criteria.
                        </span>

                    </div>

                </td>

            </tr>

        `;

        return;

    }


    tbody.innerHTML =
        jobs
            .map(
                createJobRow
            )
            .join("");

}


/* ============================================================
 * CREATE JOB ROW
 * ============================================================
 */

function createJobRow(
    job
) {

    const records =
        Number(
            job.totalRecords || 0
        );


    const success =
        Number(
            job.successRecords || 0
        );


    return `

        <tr>

            <td>

                <span
                    class="history-job-id"
                    title="${escapeHtml(
                        job.jobId || "-"
                    )}">

                    ${escapeHtml(
                        shortenJobId(
                            job.jobId
                        )
                    )}

                </span>

            </td>


            <td>

                <span
                    class="history-module">

                    ${escapeHtml(
                        formatModuleName(
                            job.module
                        )
                    )}

                </span>

            </td>


            <td>

                ${escapeHtml(
                    job.tenant || "-"
                )}

            </td>


            <td>

                ${getStatusBadge(
                    job.status
                )}

            </td>


            <td>

                <div class="record-summary">

                    <strong>
                        ${formatNumber(
                            records
                        )}
                    </strong>

                    <span>
                        ${formatNumber(
                            success
                        )}
                        successful
                    </span>

                </div>

            </td>


            <td>

                ${formatDateTime(
                    job.startedAt
                )}

            </td>


            <td>

                ${calculateDuration(
                    job.startedAt,
                    job.completedAt
                )}

            </td>


            <td>

                ${renderProgress(
                    job.progressPercent
                )}

            </td>

        </tr>

    `;

}


/* ============================================================
 * STATUS BADGE
 * ============================================================
 */

function getStatusBadge(
    status
) {

    const normalized =
        String(
            status || ""
        ).toUpperCase();


    switch (normalized) {

        case "COMPLETED":

            return `
                <span class="history-status success">
                    <i class="fa-solid fa-circle-check"></i>
                    Success
                </span>
            `;


        case "COMPLETED_WITH_ERRORS":

            return `
                <span class="history-status warning">
                    <i class="fa-solid fa-triangle-exclamation"></i>
                    Completed with Errors
                </span>
            `;


        case "FAILED":

            return `
                <span class="history-status failed">
                    <i class="fa-solid fa-circle-xmark"></i>
                    Failed
                </span>
            `;


        case "RUNNING":

            return `
                <span class="history-status running">
                    <i class="fa-solid fa-spinner fa-spin"></i>
                    Running
                </span>
            `;


        case "PROCESSING":

            return `
                <span class="history-status running">
                    <i class="fa-solid fa-spinner fa-spin"></i>
                    Processing
                </span>
            `;


        case "PENDING":

            return `
                <span class="history-status running">
                    <i class="fa-solid fa-clock"></i>
                    Pending
                </span>
            `;


        default:

            return `
                <span class="history-status">
                    ${escapeHtml(
                        status || "-"
                    )}
                </span>
            `;

    }

}


/* ============================================================
 * PROGRESS
 * ============================================================
 */

function renderProgress(
    progress
) {

    let value =
        Number(
            progress || 0
        );


    if (
        !Number.isFinite(value)
    ) {

        value = 0;

    }


    value =
        Math.max(
            0,
            Math.min(
                100,
                value
            )
        );


    return `

        <div class="progress-wrapper">

            <div class="progress">

                <div
                    class="progress-bar"
                    style="width:${value}%">
                </div>

            </div>

            <span>
                ${value}%
            </span>

        </div>

    `;

}


/* ============================================================
 * ACTIVITY CHART
 * ============================================================
 */

function renderActivityChart(
    jobs
) {

    const canvas =
        document.getElementById(
            "historyActivityChart"
        );


    if (!canvas) {

        return;

    }


    if (
        typeof Chart ===
        "undefined"
    ) {

        console.error(
            "Chart.js is not loaded."
        );

        return;

    }


    if (historyChart) {

        historyChart.destroy();

        historyChart = null;

    }


    const grouped =
        groupJobsByDate(
            jobs
        );


    const labels =
        Object.keys(
            grouped
        );


    const successful =
        labels.map(
            function (label) {

                return grouped[
                    label
                ].successful;

            }
        );


    const failed =
        labels.map(
            function (label) {

                return grouped[
                    label
                ].failed;

            }
        );


    historyChart =
        new Chart(
            canvas,
            {

                type: "bar",

                data: {

                    labels: labels,

                    datasets: [

                        {
                            label:
                                "Successful",

                            data:
                                successful,

                            backgroundColor:
                                "rgba(34, 197, 94, 0.52)",

                            borderColor:
                                "rgba(34, 197, 94, 0.95)",

                            borderWidth: 1.5,

                            borderRadius: 7,

                            borderSkipped: false
                        },


                        {
                            label:
                                "Failed / Errors",

                            data:
                                failed,

                            backgroundColor:
                                "rgba(239, 68, 68, 0.42)",

                            borderColor:
                                "rgba(239, 68, 68, 0.92)",

                            borderWidth: 1.5,

                            borderRadius: 7,

                            borderSkipped: false
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


                    plugins: {

                        legend: {

                            display: true,

                            position: "top",

                            align: "end",

                            labels: {

                                usePointStyle: true,

                                pointStyle:
                                    "circle",

                                padding: 16,

                                font: {
                                    size: 10
                                }

                            }

                        },


                        tooltip: {

                            enabled: true

                        }

                    },


                    scales: {

                        x: {

                            stacked: true,

                            grid: {
                                display: false
                            },

                            ticks: {

                                color:
                                    "#738197",

                                font: {
                                    size: 9
                                }

                            }

                        },


                        y: {

                            stacked: true,

                            beginAtZero: true,

                            ticks: {

                                precision: 0,

                                color:
                                    "#738197",

                                font: {
                                    size: 9
                                }

                            },

                            grid: {

                                color:
                                    "rgba(148, 163, 184, 0.15)",

                                drawBorder:
                                    false

                            }

                        }

                    }

                }

            }
        );

}


/* ============================================================
 * GROUP BY DATE
 * ============================================================
 */

function groupJobsByDate(
    jobs
) {

    const grouped = {};


    jobs.forEach(
        function (job) {

            if (!job.startedAt) {

                return;

            }


            const date =
                new Date(
                    job.startedAt
                );


            if (
                Number.isNaN(
                    date.getTime()
                )
            ) {

                return;

            }


            const key =
                date.toLocaleDateString(
                    "en-IN",
                    {
                        day: "2-digit",
                        month: "short"
                    }
                );


            if (!grouped[key]) {

                grouped[key] = {

                    successful: 0,

                    failed: 0

                };

            }


            const status =
                String(
                    job.status || ""
                ).toUpperCase();


            if (
                status ===
                "COMPLETED"
            ) {

                grouped[
                    key
                ].successful++;


            } else if (

                status ===
                    "FAILED"

                ||

                status ===
                    "COMPLETED_WITH_ERRORS"

            ) {

                grouped[
                    key
                ].failed++;

            }

        }
    );


    return grouped;

}


/* ============================================================
 * PAGINATION
 * ============================================================
 */

function renderPagination(
    data
) {

    const container =
        document.getElementById(
            "paginationControls"
        );


    if (!container) {

        return;

    }


    container.innerHTML = "";


    const totalPages =
        Number(
            data.totalPages || 0
        );


    if (
        totalPages <= 1
    ) {

        return;

    }


    container.appendChild(
        createPaginationButton(
            "‹",
            currentPage > 0,
            function () {

                currentPage--;

                loadHistory();

            }
        )
    );


    const pages =
        buildVisiblePages(
            totalPages,
            currentPage
        );


    pages.forEach(
        function (page) {

            if (
                page === "..."
            ) {

                const dots =
                    document.createElement(
                        "span"
                    );

                dots.className =
                    "pagination-btn";

                dots.textContent =
                    "...";

                container.appendChild(
                    dots
                );

                return;

            }


            const button =
                createPaginationButton(
                    String(
                        page + 1
                    ),
                    true,
                    function () {

                        currentPage =
                            page;

                        loadHistory();

                    }
                );


            if (
                page ===
                currentPage
            ) {

                button.classList.add(
                    "active"
                );

            }


            container.appendChild(
                button
            );

        }
    );


    container.appendChild(
        createPaginationButton(
            "›",
            currentPage <
                totalPages - 1,
            function () {

                currentPage++;

                loadHistory();

            }
        )
    );

}


/* ============================================================
 * VISIBLE PAGES
 * ============================================================
 */

function buildVisiblePages(
    totalPages,
    currentPage
) {

    if (
        totalPages <= 7
    ) {

        return Array.from(
            {
                length:
                    totalPages
            },
            function (_, index) {

                return index;

            }
        );

    }


    const pages = [];

    pages.push(0);


    if (
        currentPage > 3
    ) {

        pages.push("...");

    }


    const start =
        Math.max(
            1,
            currentPage - 1
        );


    const end =
        Math.min(
            totalPages - 2,
            currentPage + 1
        );


    for (
        let i = start;
        i <= end;
        i++
    ) {

        pages.push(i);

    }


    if (
        currentPage <
        totalPages - 4
    ) {

        pages.push("...");

    }


    pages.push(
        totalPages - 1
    );


    return pages;

}


/* ============================================================
 * PAGINATION BUTTON
 * ============================================================
 */

function createPaginationButton(
    text,
    enabled,
    callback
) {

    const button =
        document.createElement(
            "button"
        );


    button.type =
        "button";


    button.className =
        "pagination-btn";


    button.textContent =
        text;


    button.disabled =
        !enabled;


    if (enabled) {

        button.addEventListener(
            "click",
            callback
        );

    }


    return button;

}


/* ============================================================
 * RESULT LABEL
 * ============================================================
 */

function updateResultLabel(
    data
) {

    const jobs =
        Array.isArray(data.jobs)
            ? data.jobs
            : [];


    const totalJobs =
        Number(
            data.totalJobs || 0
        );


    const totalPages =
        Number(
            data.totalPages || 0
        );


    setText(
        "historyResultLabel",
        "Showing "
        +
        formatNumber(
            jobs.length
        )
        +
        " of "
        +
        formatNumber(
            totalJobs
        )
        +
        " jobs"
    );


    if (totalPages > 0) {

        setText(
            "paginationSummary",
            "Page "
            +
            (currentPage + 1)
            +
            " of "
            +
            totalPages
        );

    } else {

        setText(
            "paginationSummary",
            "No pages"
        );

    }

}


/* ============================================================
 * RESET
 * ============================================================
 */

function resetFilters() {

    setValue(
        "filterJobId",
        ""
    );


    setValue(
        "filterFromDate",
        ""
    );


    setValue(
        "filterToDate",
        ""
    );


    if (moduleSelect) {

        moduleSelect.setValue(
            "ALL",
            true
        );

    }


    if (statusSelect) {

        statusSelect.setValue(
            "ALL",
            true
        );

    }


    if (tenantSelect) {

        tenantSelect.setValue(
            "ALL",
            true
        );

    }


    currentPage = 0;


    loadHistory();

}


/* ============================================================
 * EXPORT
 * ============================================================
 */

function exportHistory() {

    const params =
        buildFilterParams();


    const query =
        params.toString();


    const url =
        getContextPath()
        +
        "/migration/history/export"
        +
        (
            query
                ? "?" + query
                : ""
        );


    console.log(
        "Export URL:",
        url
    );


    window.location.href =
        url;

}


/* ============================================================
 * LOADING
 * ============================================================
 */

function showTableLoading() {

    const tbody =
        document.getElementById(
            "historyTableBody"
        );


    if (!tbody) {

        return;

    }


    tbody.innerHTML = `

        <tr>

            <td colspan="8">

                <div class="history-loading">

                    <i class="fa-solid fa-spinner fa-spin"></i>

                    Loading migration history...

                </div>

            </td>

        </tr>

    `;

}


/* ============================================================
 * ERROR
 * ============================================================
 */

function showTableError() {

    const tbody =
        document.getElementById(
            "historyTableBody"
        );


    if (!tbody) {

        return;

    }


    tbody.innerHTML = `

        <tr>

            <td colspan="8">

                <div class="history-empty">

                    <div class="history-empty-icon">

                        <i class="fa-solid fa-triangle-exclamation"></i>

                    </div>


                    <strong>
                        Unable to load migration history
                    </strong>


                    <span>
                        Please check the server connection
                        and try again.
                    </span>

                </div>

            </td>

        </tr>

    `;

}


/* ============================================================
 * SET VALUE
 * ============================================================
 */

function setValue(
    elementId,
    value
) {

    const element =
        document.getElementById(
            elementId
        );


    if (element) {

        element.value =
            value;

    }

}


/* ============================================================
 * SET TEXT
 * ============================================================
 */

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


/* ============================================================
 * FORMAT NUMBER
 * ============================================================
 */

function formatNumber(
    value
) {

    const number =
        Number(
            value || 0
        );


    if (
        !Number.isFinite(
            number
        )
    ) {

        return "0";

    }


    return number.toLocaleString(
        "en-IN"
    );

}


/* ============================================================
 * FORMAT MODULE
 * ============================================================
 */

function formatModuleName(
    module
) {

    if (!module) {

        return "-";

    }


    return String(
        module
    )
        .toLowerCase()
        .split("_")
        .map(
            function (word) {

                return (
                    word.charAt(0).toUpperCase()
                    +
                    word.slice(1)
                );

            }
        )
        .join(" ");

}


/* ============================================================
 * SHORTEN JOB ID
 * ============================================================
 */

function shortenJobId(
    jobId
) {

    if (!jobId) {

        return "-";

    }


    if (
        jobId.length <= 16
    ) {

        return jobId;

    }


    return (
        jobId.substring(
            0,
            8
        )
        +
        "..."
        +
        jobId.substring(
            jobId.length - 4
        )
    );

}


/* ============================================================
 * DATE FORMAT
 * ============================================================
 */

function formatDateTime(
    value
) {

    if (!value) {

        return "-";

    }


    const date =
        new Date(
            value
        );


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

            year: "numeric",

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
        new Date(
            startedAt
        );


    if (
        Number.isNaN(
            start.getTime()
        )
    ) {

        return "-";

    }


    const end =
        completedAt
            ? new Date(completedAt)
            : new Date();


    const difference =
        Math.max(
            0,
            end.getTime()
            -
            start.getTime()
        );


    const totalSeconds =
        Math.floor(
            difference / 1000
        );


    const hours =
        Math.floor(
            totalSeconds / 3600
        );


    const minutes =
        Math.floor(
            (
                totalSeconds % 3600
            ) / 60
        );


    const seconds =
        totalSeconds % 60;


    if (hours > 0) {

        return (
            hours
            +
            "h "
            +
            minutes
            +
            "m "
            +
            seconds
            +
            "s"
        );

    }


    if (minutes > 0) {

        return (
            minutes
            +
            "m "
            +
            seconds
            +
            "s"
        );

    }


    return seconds + "s";

}


/* ============================================================
 * ESCAPE HTML
 * ============================================================
 */

function escapeHtml(
    value
) {

    if (
        value === null ||
        value === undefined
    ) {

        return "";

    }


    return String(
        value
    )
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