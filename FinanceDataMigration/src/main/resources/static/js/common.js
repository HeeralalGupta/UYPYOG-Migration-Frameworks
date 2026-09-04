/*
 * ============================================================
 * Finance Data Migration Framework
 * Common JavaScript
 * ============================================================
 */


/* ============================================================
 * CONTEXT PATH
 * ============================================================ */

function getContextPath() {
    return window.contextPath || '';
}


/* ============================================================
 * MIGRATION USER / TENANT
 *
 * Finance application opens:
 *
 * /migration?ms_tenant_id=hr.gurugram&username=Gurugram
 *
 * We store these values in sessionStorage so they remain
 * available when the user navigates to other migration pages.
 * ============================================================ */

function getMigrationUser() {

    const params =
        new URLSearchParams(window.location.search);

    /*
     * Read values from URL
     */
    let tenantId =
        params.get("ms_tenant_id");

    let username =
        params.get("username");


    /*
     * If URL contains tenant, save it
     */
    if (tenantId && tenantId.trim() !== "") {

        sessionStorage.setItem(
            "migrationTenantId",
            tenantId.trim()
        );
    }


    /*
     * If URL contains username, save it
     */
    if (username && username.trim() !== "") {

        sessionStorage.setItem(
            "migrationUsername",
            username.trim()
        );
    }


    /*
     * For other pages, read from sessionStorage
     */
    tenantId =
        tenantId ||
        sessionStorage.getItem(
            "migrationTenantId"
        );

    username =
        username ||
        sessionStorage.getItem(
            "migrationUsername"
        );


    return {
        tenantId: tenantId,
        username: username
    };
}


/* ============================================================
 * FORMAT TENANT NAME
 *
 * hr.gurugram -> Gurugram
 * ============================================================ */

function formatTenantName(tenantId) {

    if (!tenantId) {
        return '';
    }

    let name = tenantId;

    if (name.startsWith("hr.")) {
        name = name.substring(3);
    }

    if (!name) {
        return '';
    }

    return (
        name.charAt(0).toUpperCase() +
        name.slice(1)
    );
}


/* ============================================================
 * APPLY USER / TENANT TO PAGE
 *
 * Every page that loads common.js will get:
 *
 * - username
 * - tenant
 * - locked tenant dropdown
 * ============================================================ */

function applyMigrationUser() {

    const user =
        getMigrationUser();

    console.log(
        "Migration User:",
        user
    );


    /*
     * ========================================================
     * USERNAME
     * ========================================================
     */

    document
        .querySelectorAll(
            '[data-migration-username]'
        )
        .forEach(function(element) {

            element.textContent =
                user.username || "User";

        });


    /*
     * ========================================================
     * TENANT DROPDOWNS
     * ========================================================
     *
     * Add:
     *
     * data-tenant-dropdown
     *
     * to any tenant <select>.
     */

    document
        .querySelectorAll(
            '[data-tenant-dropdown]'
        )
        .forEach(function(select) {

            /*
             * No tenant available
             */
            if (!user.tenantId) {

                console.warn(
                    "Migration tenant is not available."
                );

                select.innerHTML = `
                    <option value="">
                        Tenant not provided
                    </option>
                `;

                select.disabled = true;

                return;
            }


            /*
             * Display name
             */
            const displayName =
                formatTenantName(
                    user.tenantId
                );


            /*
             * Show ONLY Finance-provided tenant
             */
            select.innerHTML = `
                <option
                    value="${escapeHtmlAttribute(user.tenantId)}"
                    selected>
                    ${escapeHtml(displayName)}
                </option>
            `;


            /*
             * Set actual value
             */
            select.value =
                user.tenantId;


            /*
             * Lock the tenant dropdown
             */
            select.disabled = true;


            console.log(
                "Tenant dropdown locked:",
                user.tenantId
            );

        });
}


/* ============================================================
 * HTML ESCAPE HELPERS
 * ============================================================ */

function escapeHtml(value) {

    if (value === null || value === undefined) {
        return '';
    }

    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}


function escapeHtmlAttribute(value) {

    return escapeHtml(value);
}


/* ============================================================
 * GLOBAL TOAST
 * ============================================================ */

function createGlobalToast() {

    /*
     * Already created
     */
    if (
        document.getElementById(
            "globalErrorToast"
        )
    ) {
        return;
    }


    const toast =
        document.createElement("div");


    toast.id =
        "globalErrorToast";


    toast.className =
        "global-toast error";


    toast.innerHTML = `
        <div class="global-toast-icon">
            !
        </div>

        <div class="global-toast-content">

            <div class="global-toast-title">
                Error
            </div>

            <div
                class="global-toast-message"
                id="globalToastMessage">
            </div>

        </div>

        <button
            type="button"
            class="global-toast-close"
            onclick="hideGlobalToast()">
            &times;
        </button>
    `;


    document.body.appendChild(
        toast
    );
}


/* ============================================================
 * SHOW ERROR
 * ============================================================ */

function showGlobalError(
    message,
    title
) {

    createGlobalToast();


    const toast =
        document.getElementById(
            "globalErrorToast"
        );


    const messageElement =
        document.getElementById(
            "globalToastMessage"
        );


    const titleElement =
        toast.querySelector(
            ".global-toast-title"
        );


    /*
     * Reset classes
     */
    toast.classList.remove(
        "success",
        "error"
    );

    toast.classList.add(
        "error"
    );


    /*
     * Set content
     */
    titleElement.textContent =
        title || "Error";


    messageElement.textContent =
        message ||
        "Something went wrong.";


    /*
     * Show toast
     */
    toast.classList.add(
        "show"
    );


    /*
     * Clear old timer
     */
    clearTimeout(
        window.globalToastTimer
    );


    /*
     * Hide after 5 seconds
     */
    window.globalToastTimer =
        setTimeout(function() {

            hideGlobalToast();

        }, 5000);
}


/* ============================================================
 * SHOW SUCCESS
 * ============================================================ */

function showGlobalSuccess(
    message,
    title
) {

    createGlobalToast();


    const toast =
        document.getElementById(
            "globalErrorToast"
        );


    const messageElement =
        document.getElementById(
            "globalToastMessage"
        );


    const titleElement =
        toast.querySelector(
            ".global-toast-title"
        );


    /*
     * Reset classes
     */
    toast.classList.remove(
        "success",
        "error"
    );

    toast.classList.add(
        "success"
    );


    /*
     * Change icon
     */
    toast.querySelector(
        ".global-toast-icon"
    ).textContent = "✓";


    /*
     * Set content
     */
    titleElement.textContent =
        title || "Success";


    messageElement.textContent =
        message ||
        "Operation completed successfully.";


    /*
     * Show toast
     */
    toast.classList.add(
        "show"
    );


    /*
     * Clear old timer
     */
    clearTimeout(
        window.globalToastTimer
    );


    /*
     * Hide after 5 seconds
     */
    window.globalToastTimer =
        setTimeout(function() {

            hideGlobalToast();

        }, 5000);
}


/* ============================================================
 * HIDE GLOBAL TOAST
 * ============================================================ */

function hideGlobalToast() {

    const toast =
        document.getElementById(
            "globalErrorToast"
        );

    if (toast) {

        toast.classList.remove(
            "show"
        );
    }
}


/*
 * Backward compatibility
 *
 * If any existing HTML is using:
 *
 * onclick="hideGlobalError()"
 *
 * it will continue to work.
 */

function hideGlobalError() {

    hideGlobalToast();
}


/* ============================================================
 * PARSE API ERROR
 * ============================================================ */

async function parseApiError(
    response
) {

    try {

        const data =
            await response.json();


        return {

            message:
                data.message ||
                data.error ||
                "Something went wrong.",

            title:
                data.error ||
                "Error"

        };

    } catch (error) {

        return {

            message:
                "Unable to process server response.",

            title:
                "Server Error"

        };
    }
}


/* ============================================================
 * COMMON API FETCH
 *
 * Use this for normal API calls.
 *
 * Do NOT use this for migration progress polling if you want
 * polling failures to remain silent.
 * ============================================================ */

async function apiFetch(
    url,
    options = {}
) {

    try {

        const response =
            await fetch(
                url,
                options
            );


        if (!response.ok) {

            const error =
                await parseApiError(
                    response
                );


            showGlobalError(
                error.message,
                error.title
            );


            throw new Error(
                error.message
            );
        }


        return response;

    } catch (error) {

        /*
         * Error was already handled above
         */
        if (error && error.message) {
            throw error;
        }


        showGlobalError(
            "Unable to connect to the server.",
            "Connection Error"
        );


        throw error;
    }
}


/* ============================================================
 * MIGRATION PROGRESS MONITOR
 *
 * Existing backend endpoint:
 *
 * GET /migration/progress/{jobId}
 * ============================================================ */

function monitorMigration(
    jobId
) {

    if (!jobId) {

        console.error(
            "Cannot monitor migration without Job ID."
        );

        return;
    }


    console.log(
        "Starting migration progress polling:",
        jobId
    );


    /*
     * Check immediately once
     */
    checkMigrationProgress(
        jobId
    );


    /*
     * Then check every 2 seconds
     */
    const interval =
        setInterval(
            async function() {

                const finished =
                    await checkMigrationProgress(
                        jobId
                    );

                /*
                 * Stop polling after SUCCESS / FAILED
                 */
                if (finished) {

                    clearInterval(
                        interval
                    );
                }

            },
            2000
        );


    /*
     * Return interval in case page-specific JS
     * wants to stop it manually.
     */
    return interval;
}


/* ============================================================
 * CHECK MIGRATION PROGRESS
 * ============================================================ */

async function checkMigrationProgress(
    jobId
) {

    try {

        const response =
            await fetch(

                getContextPath() +
                "/migration/progress/" +
                encodeURIComponent(jobId),

                {
                    method: "GET",
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );


        /*
         * Don't show toast for temporary polling errors.
         */
        if (!response.ok) {

            console.warn(
                "Migration progress API returned:",
                response.status
            );

            return false;
        }


        const progress =
            await response.json();


        console.log(
            "Migration progress:",
            progress
        );


        /*
         * Update progress UI
         * if page has this function.
         */
        if (
            typeof updateMigrationProgress ===
            "function"
        ) {

            updateMigrationProgress(
                progress
            );
        }


        /*
         * =====================================================
         * SUCCESS
         * =====================================================
         */

        if (
            progress.status ===
            "SUCCESS"
        ) {

            showGlobalSuccess(
                progress.currentMessage ||
                "Migration completed successfully."
            );

            return true;
        }


        /*
         * =====================================================
         * FAILED
         * =====================================================
         */

        if (
            progress.status ===
            "FAILED"
        ) {

            showGlobalError(
                progress.currentMessage ||
                "Migration failed.",
                "Migration Failed"
            );

            return true;
        }


        /*
         * PROCESSING / RUNNING
         */
        return false;


    } catch (error) {

        console.error(
            "Error while checking migration progress:",
            error
        );


        /*
         * Don't stop polling because of one
         * temporary network error.
         */
        return false;
    }
}


/* ============================================================
 * INITIALIZE COMMON UI
 * ============================================================ */

document.addEventListener(
    "DOMContentLoaded",
    function() {

        console.log(
            "Common JS initialized"
        );


        /*
         * Apply Finance user / tenant
         */
        applyMigrationUser();

    }
);