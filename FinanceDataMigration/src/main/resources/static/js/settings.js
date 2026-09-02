"use strict";


/* ============================================================
 * SETTINGS PAGE
 * ============================================================
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        initializeSettings();
		initializeSettingsControls();

    }
);


function initializeSettingsControls() {

    const saveButton =
        document.getElementById(
            "saveSettingsBtn"
        );


    if (saveButton) {

        saveButton.addEventListener(
            "click",
            saveSettings
        );

    }


    const resetButton =
        document.getElementById(
            "resetSettingsBtn"
        );


    if (resetButton) {

        resetButton.addEventListener(
            "click",
            loadSettings
        );

    }

}
/* ============================================================
 * INITIALIZE
 * ============================================================
 */

function initializeSettings() {

    loadSettings();

    updateCurrentTime();

    setInterval(
        updateCurrentTime,
        1000
    );

}


/* ============================================================
 * LOAD SETTINGS
 * ============================================================
 */

async function loadSettings() {

    try {

        const response =
            await fetch(
                getContextPath()
                + "/migration/settings/data",
                {
                    method: "GET",

                    headers: {
                        "Accept":
                            "application/json"
                    },

                    cache: "no-store"
                }
            );


        console.log(
            "Settings API status:",
            response.status
        );


        if (!response.ok) {

            throw new Error(
                "Settings API returned HTTP "
                + response.status
            );

        }


        const data =
            await response.json();


        console.log(
            "Settings API response:",
            data
        );


        renderSettings(
            data
        );


    } catch (error) {

        console.error(
            "Unable to load settings:",
            error
        );


        showSettingsError();

    }

}


/* ============================================================
 * RENDER SETTINGS
 * ============================================================
 */

function renderSettings(data) {

    if (!data) {

        console.error(
            "Settings data is empty."
        );

        return;
    }
	
	setValue(
	    "defaultPageSize",
	    data.defaultPageSize
	);


	setValue(
	    "autoRefreshSeconds",
	    data.autoRefreshSeconds
	);


	setValue(
	    "maxUploadSizeMb",
	    data.maxUploadSizeMb
	);


	setValue(
	    "allowedFileExtensions",
	    data.allowedFileExtensions
	);


	setValue(
	    "duplicatePolicy",
	    data.duplicatePolicy
	);


	setValue(
	    "concurrentMigrationLimit",
	    data.concurrentMigrationLimit
	);


    /*
     * =====================================================
     * BASIC INFORMATION
     * =====================================================
     */

    setText(
        "applicationName",
        data.applicationName || "-"
    );

    setText(
        "applicationStatus",
        data.applicationStatus || "Unknown"
    );

    setText(
        "databaseStatus",
        data.databaseStatus || "Unknown"
    );

    setText(
        "migrationEngineStatus",
        data.migrationEngineStatus || "Unknown"
    );


    /*
     * =====================================================
     * COUNTS
     * =====================================================
     */

    setText(
        "activeJobs",
        formatNumber(
            data.activeJobs
        )
    );

    setText(
        "totalModules",
        formatNumber(
            data.totalModules ??
            (
                Array.isArray(data.modules)
                    ? data.modules.length
                    : 0
            )
        )
    );

    setText(
        "tenantCount",
        formatNumber(
            data.tenantCount ??
            (
                Array.isArray(data.tenants)
                    ? data.tenants.length
                    : 0
            )
        )
    );


    /*
     * =====================================================
     * CONFIGURATION
     * =====================================================
     */

    setText(
        "defaultPageSize",
        formatNumber(
            data.defaultPageSize
        )
    );

    setText(
        "autoRefresh",
        (
            data.autoRefreshSeconds ??
            0
        )
        +
        " sec"
    );


    /*
     * =====================================================
     * STATUS
     * =====================================================
     */

    updateStatusDot(
        "applicationStatusDot",
        data.applicationStatus
    );

    updateStatusDot(
        "databaseStatusDot",
        data.databaseStatus
    );

    updateStatusDot(
        "migrationEngineStatusDot",
        data.migrationEngineStatus
    );


    /*
     * =====================================================
     * MODULES
     * =====================================================
     */

    renderSettingsModules(
        Array.isArray(data.modules)
            ? data.modules
            : []
    );


    /*
     * =====================================================
     * TENANTS
     * =====================================================
     */

    renderSettingsTenants(
        Array.isArray(data.tenants)
            ? data.tenants
            : []
    );
}


/* ============================================================
 * RENDER MODULES
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
            value ?? "";

    }

}

async function saveSettings() {

    const request = {

        defaultPageSize:
            Number(
                document.getElementById(
                    "defaultPageSize"
                ).value
            ),

        autoRefreshSeconds:
            Number(
                document.getElementById(
                    "autoRefreshSeconds"
                ).value
            ),

        maxUploadSizeMb:
            Number(
                document.getElementById(
                    "maxUploadSizeMb"
                ).value
            ),

        allowedFileExtensions:
            document.getElementById(
                "allowedFileExtensions"
            ).value.trim(),

        duplicatePolicy:
            document.getElementById(
                "duplicatePolicy"
            ).value,

        concurrentMigrationLimit:
            Number(
                document.getElementById(
                    "concurrentMigrationLimit"
                ).value
            )

    };


    try {

        const response =
            await fetch(
                getContextPath()
                +
                "/migration/settings/config",
                {

                    method: "PUT",

                    headers: {

                        "Content-Type":
                            "application/json",

                        "Accept":
                            "application/json"

                    },

                    body:
                        JSON.stringify(
                            request
                        )

                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            throw new Error(
                data.message
                ||
                "Unable to save settings"
            );

        }


        renderSettings(
            data
        );


        showSettingsMessage(
            "Settings saved successfully.",
            "success"
        );


    } catch (error) {

        console.error(
            "Unable to save settings:",
            error
        );


        showSettingsMessage(
            error.message,
            "error"
        );

    }

}

function showSettingsMessage(
    message,
    type
) {

    let container =
        document.getElementById(
            "settingsMessage"
        );


    if (!container) {

        container =
            document.createElement(
                "div"
            );

        container.id =
            "settingsMessage";

        container.className =
            "settings-message";

        document
            .querySelector(
                ".settings-page"
            )
            .prepend(
                container
            );

    }


    container.className =
        "settings-message "
        +
        type;


    container.innerHTML = `

        <i class="${
            type === "success"
                ? "fa-solid fa-circle-check"
                : "fa-solid fa-circle-exclamation"
        }"></i>

        <span>
            ${escapeHtml(message)}
        </span>

    `;


    setTimeout(
        function () {

            container.remove();

        },
        4000
    );

}

function renderSettingsModules(modules) {

    const container =
        document.getElementById(
            "moduleList"
        );


    if (!container) {

        console.error(
            "Settings element #moduleList was not found."
        );

        return;
    }


    if (
        !Array.isArray(modules) ||
        modules.length === 0
    ) {

        container.innerHTML = `

            <div class="settings-empty">

                <i class="fa-solid fa-layer-group"></i>

                No migration modules configured.

            </div>

        `;

        return;
    }


    container.innerHTML =
        modules
            .map(
                function (module) {

                    return `

                        <div class="module-item">

                            <div class="module-item-icon">

                                <i class="fa-solid fa-layer-group"></i>

                            </div>

                            <span>
                                ${escapeHtml(
                                    formatModule(
                                        module
                                    )
                                )}
                            </span>

                            <i
                                class="fa-solid fa-circle-check module-enabled">
                            </i>

                        </div>

                    `;

                }
            )
            .join("");
}


/* ============================================================
 * RENDER TENANTS
 * ============================================================
 */

function renderSettingsTenants(tenants) {

    const container =
        document.getElementById(
            "tenantList"
        );


    if (!container) {

        console.error(
            "Settings element #tenantList was not found."
        );

        return;
    }


    if (
        !Array.isArray(tenants) ||
        tenants.length === 0
    ) {

        container.innerHTML = `

            <div class="settings-empty">

                <i class="fa-solid fa-building"></i>

                No tenants configured.

            </div>

        `;

        return;
    }


    container.innerHTML =
        tenants
            .map(
                function (tenant) {

                    const value =
                        String(
                            tenant
                        ).trim();


                    return `

                        <div class="tenant-item">

                            <div class="tenant-item-icon">

                                <i class="fa-solid fa-building"></i>

                            </div>

                            <div class="tenant-item-content">

                                <strong>
                                    ${escapeHtml(
                                        formatTenant(
                                            value
                                        )
                                    )}
                                </strong>

                                <small>
                                    ${escapeHtml(
                                        value
                                    )}
                                </small>

                            </div>

                            <span class="tenant-active">
                                Active
                            </span>

                        </div>

                    `;

                }
            )
            .join("");
}


/* ============================================================
 * STATUS DOT
 * ============================================================
 */

function updateStatusDot(
    elementId,
    status
) {

    const element =
        document.getElementById(
            elementId
        );


    if (!element) {

        return;

    }


    const normalized =
        String(
            status || ""
        )
        .trim()
        .toUpperCase();


    element.classList.remove(
        "green",
        "red",
        "orange",
        "gray"
    );


    if (
        normalized === "RUNNING"
        ||
        normalized === "CONNECTED"
        ||
        normalized === "AVAILABLE"
    ) {

        element.classList.add(
            "green"
        );

        return;

    }


    if (
        normalized === "ERROR"
        ||
        normalized === "FAILED"
        ||
        normalized === "DISCONNECTED"
        ||
        normalized === "UNAVAILABLE"
    ) {

        element.classList.add(
            "red"
        );

        return;

    }


    if (
        normalized === "STARTING"
        ||
        normalized === "PROCESSING"
    ) {

        element.classList.add(
            "orange"
        );

        return;

    }


    element.classList.add(
        "gray"
    );

}


/* ============================================================
 * MODULE FORMAT
 * ============================================================
 */

function formatModule(
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
                    word.charAt(0)
                    .toUpperCase()
                    +
                    word.slice(1)
                );

            }
        )
        .join(" ");

}


/* ============================================================
 * TENANT FORMAT
 *
 * hr.gurugram -> Gurugram
 * hr.ambala   -> Ambala
 * ============================================================
 */

function formatTenant(
    tenant
) {

    if (!tenant) {

        return "";

    }


    const value =
        String(
            tenant
        )
        .trim();


    const parts =
        value.split(
            "."
        );


    const name =
        parts.length > 1
            ? parts
                .slice(1)
                .join(".")
            : parts[0];


    if (!name) {

        return "";

    }


    return (
        name.charAt(0).toUpperCase()
        +
        name.slice(1).toLowerCase()
    );

}


/* ============================================================
 * NUMBER
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
 * CURRENT TIME
 * ============================================================
 */

function updateCurrentTime() {

    setText(
        "currentTime",
        new Date().toLocaleString(
            "en-IN",
            {
                day: "2-digit",

                month: "short",

                year: "numeric",

                hour: "2-digit",

                minute: "2-digit",

                second: "2-digit"
            }
        )
    );

}


/* ============================================================
 * ERROR STATE
 * ============================================================
 */

function showSettingsError() {

    setText(
        "applicationStatus",
        "Unavailable"
    );


    setText(
        "databaseStatus",
        "Unavailable"
    );


    setText(
        "migrationEngineStatus",
        "Unavailable"
    );


    const moduleList =
        document.getElementById(
            "moduleList"
        );


    if (moduleList) {

        moduleList.innerHTML = `

            <div class="settings-empty">

                Unable to load modules.

            </div>

        `;

    }


    const tenantList =
        document.getElementById(
            "tenantList"
        );


    if (tenantList) {

        tenantList.innerHTML = `

            <div class="settings-empty">

                Unable to load tenants.

            </div>

        `;

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


    if (!element) {

        console.warn(
            "Element not found:",
            elementId
        );

        return;

    }


    element.textContent =
        value;

}


/* ============================================================
 * CONTEXT PATH
 * ============================================================
 */

function getContextPath() {

    return window.contextPath
        || "";

}


/* ============================================================
 * ESCAPE HTML
 * ============================================================
 */

function escapeHtml(
    value
) {

    if (
        value === null
        ||
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