/**
 * 
 */
const moduleData = [
    {
        category: "Finance Masters",
        modules: [
            {
                code: "BANK",
                name: "Bank",
                icon: "fa-building-columns",
                description: "Import Bank Master",
                version: "1.0",
                color: "success",
                moduleType: "BANK",
                status: "ACTIVE",
                route: "/migration/upload/BANK",
                template: "Bank_Template.xlsx"
            },
            {
                code: "BANK_BRANCH",
                name: "Bank Branch",
                icon: "fa-code-branch",
                description: "Import Bank Branch",
                version: "1.0",
                color: "warning",
                moduleType: "BANK_BRANCH",
                status: "ACTIVE",
                route: "/migration/upload/BANK_BRANCH",
                template: "Bank_Branch_Template.xlsx"
            },
            {
                code: "BANK_ACCOUNT",
                name: "Bank Account",
                icon: "fa-credit-card",
                description: "Import Bank Accounts",
                version: "1.0",
                color: "danger",
                moduleType: "BANK_ACCOUNT",
                status: "ACTIVE",
                route: "/migration/upload/BANK_ACCOUNT",
                template: "Bank_Account_Template.xlsx"
            },
            {
                code: "FUND",
                name: "Fund",
                icon: "fa-sack-dollar",
                description: "Import Funds",
                version: "1.0",
                color: "secondary",
                moduleType: "FUND",
                status: "ACTIVE",
                route: "/migration/upload/FUND",
                template: "Fund_Template.xlsx"
            },
            {
                code: "SCHEME",
                name: "Scheme",
                icon: "fa-diagram-project",
                description: "Import Scheme",
                version: "1.0",
                color: "secondary",
                moduleType: "SCHEME",
                status: "ACTIVE",
                route: "/migration/upload/SCHEME",
                template: "Scheme_Template.xlsx"
            },
            {
                code: "CONTRACTOR",
                name: "Contractor",
                icon: "fa-hard-hat",
                description: "Import Contractor",
                version: "1.0",
                color: "secondary",
                moduleType: "CONTRACTOR",
                status: "ACTIVE",
                route: "/migration/upload/CONTRACTOR",
                template: "Contractor_Template.xlsx"
            },
            {
                code: "SUPPLIER",
                name: "Supplier",
                icon: "fa-truck-field",
                description: "Import Supplier",
                version: "1.0",
                color: "secondary",
                moduleType: "SUPPLIER",
                status: "ACTIVE",
                route: "/migration/upload/SUPPLIER",
                template: "Supplier_Template.xlsx"
            },
            {
                code: "WORK",
                name: "Work",
                icon: "fa-briefcase",
                description: "Import Works",
                version: "1.0",
                color: "secondary",
                moduleType: "WORK",
                status: "ACTIVE",
                route: "/migration/upload/WORK",
                template: "Work_Template.xlsx"
            }
        ]
    },
    {
        category: "Order Type",
        modules: [
            {
                code: "WORK_ORDER",
                name: "Work Order",
                icon: "fa-clipboard-check",
                description: "Import Work Order",
                version: "1.0",
                color: "primary",
                moduleType: "WORK_ORDER",
                status: "ACTIVE",
                route: "/migration/upload/WORK_ORDER",
                template: "Wprk_Order_Template.xlsx"
            },
            {
                code: "PURCHASE_ORDER",
                name: "Purchase Order",
                icon: "fa-cart-shopping",
                description: "Import Purchase Order",
                version: "1.0",
                color: "primary",
                moduleType: "PURCHASE_ORDER",
                status: "ACTIVE",
                route: "/migration/upload/PURCHASE_ORDER",
                template: "Purchase_Order_Template.xlsx"
            }
        ]
    },
    {
        category: "Bills Accounting",
        modules: [
            {
                code: "CONTRACTOR_BILL",
                name: "Contractor Bill",
                icon: "fa-file-invoice-dollar",
                description: "Import Contractor Bill",
                version: "1.0",
                color: "success",
                moduleType: "FUND",
                status: "ACTIVE",
                route: "/migration/upload/FUND",
                template: "Fund_Template.xlsx"
            },
            {
                code: "EXPENSE_BILL",
                name: "Expense Bill",
                icon: "fa-receipt",
                description: "Import Expense Bill",
                version: "1.0",
                color: "warning",
                moduleType: "FUND",
                status: "ACTIVE",
                route: "/migration/upload/FUND",
                template: "Fund_Template.xlsx"
            },
            {
                code: "SUPPLIER_BILL",
                name: "Supplier Bill",
                icon: "fa-file-invoice",
                description: "Import Supplier Bill",
                version: "1.0",
                color: "warning",
                moduleType: "SUPPLIER_BILL",
                status: "ACTIVE",
                route: "/migration/upload/SUPPLIER_BILL",
                template: "Supplier_Bill_Template.xlsx"
            }
        ]
    },
    {
        category: "Financial Transaction",
        modules: [
            {
                code: "JOURNAL_VOUCHER",
                name: "Journal Voucher",
                icon: "fa-book-bookmark",
                description: "Import Journal Voucher Transactions",
                version: "1.0",
                color: "primary",
                moduleType: "JOURNAL_VOUCHER",
                status: "ACTIVE",
                route: "/migration/upload/JOURNAL_VOUCHER",
                template: "Journal_Vouhcer_Template.xlsx"
            }
        ]
    }
];

function renderModules(categories) {

    const accordion =
        document.getElementById("migrationAccordion");

    accordion.innerHTML = "";

    categories.forEach((category, index) => {

        let cards = '<div class="row g-4">';

        category.modules.forEach(module => {

            cards += createModuleCard(module);

        });

        cards += '</div>';

        accordion.innerHTML += `

<div class="accordion-item">

<h2 class="accordion-header">

<button
class="accordion-button ${index !== 0 ? 'collapsed' : ''}"
type="button"

data-bs-toggle="collapse"

data-bs-target="#collapse${index}">

${category.category}

<span class="ms-auto badge bg-light text-dark">

${category.modules.length}

</span>

</button>

</h2>

<div id="collapse${index}"

class="accordion-collapse collapse ${index === 0 ? 'show' : ''}"

data-bs-parent="#migrationAccordion">

<div class="accordion-body">

${cards}


</div>

</div>

</div>

`;

    });

}

function createModuleCard(module) {

    const disabled =
        module.status !== "ACTIVE";

    return `

<div class="col-xl-3 col-lg-4 col-md-6">

<div class="module-card ${disabled ? 'disabled-card' : ''}">

<div class="module-card-header ${module.color}">

<div class="module-icon">

<i class="fa-solid ${module.icon}"></i>

</div>

<div>

<h5 class="mb-1">

${module.name}

</h5>

<span class="status-badge
${module.status === "ACTIVE"

            ? "status-active"

            : module.status === "COMING SOON"

                ? "status-coming"

                : "status-disabled"
        }">

${module.status}

</span>

</div>

</div>

<div class="module-card-body">

<p>

${module.description}

</p>

<div class="module-details">

<div>

<i class="fa-solid fa-layer-group"></i>

${module.moduleType}

</div>

<div>

<i class="fa-solid fa-code-branch"></i>

Version ${module.version}

</div>

<div>

<i class="fa-solid fa-file-excel"></i>

${module.template}

</div>

</div>

<div class="module-actions">

<button

class="btn btn-outline-success btn-sm"

onclick="downloadTemplate('${module.template}')"

${disabled ? 'disabled' : ''}>

<i class="fa-solid fa-download"></i>

Template

</button>

<button

class="btn btn-primary btn-sm"

onclick="startMigration('${module.route}')"

${disabled ? 'disabled' : ''}>

<i class="fa-solid fa-rocket"></i>

Start

</button>

</div>

</div>

</div>

</div>

`;

}


function downloadTemplate(template) {

    window.location.href = "/templates/" + template;

}

function startMigration(route) {

    window.location.href = route;

}

document.addEventListener("DOMContentLoaded", function() {

    const accordion = document.getElementById("moduleAccordion");
    const searchInput = document.getElementById("moduleSearch");
    const clearSearch = document.getElementById("clearSearch");
    const resetSearch = document.getElementById("resetModuleSearch");
    const emptyState = document.getElementById("moduleEmptyState");
    const moduleCount = document.getElementById("moduleCount");

    if (!accordion) {
        return;
    }


    /*
     * moduleData is loaded from module-data.js
     */

    if (typeof moduleData === "undefined") {

        console.error("moduleData is not available.");

        return;

    }


    /* ==========================================
       COUNT MODULES
    ========================================== */

    let totalModules = 0;

    moduleData.forEach(function(category) {

        totalModules += category.modules.length;

    });

    moduleCount.textContent = totalModules;


    /* ==========================================
       RENDER MODULES
    ========================================== */

    function renderModules(data, searchTerm = "") {

        accordion.innerHTML = "";

        let visibleCategories = 0;

        let visibleModules = 0;


        data.forEach(function(category, categoryIndex) {

            const filteredModules = category.modules.filter(function(module) {

                if (!searchTerm) {
                    return true;
                }

                const searchText = (

                    module.name + " " +
                    module.code + " " +
                    module.description + " " +
                    category.category

                ).toLowerCase();

                return searchText.includes(searchTerm);

            });


            if (filteredModules.length === 0) {
                return;
            }


            visibleCategories++;

            visibleModules += filteredModules.length;


            const categoryDiv =
                document.createElement("div");

            categoryDiv.className = "module-category";


            /*
             * First category is open by default
             */

            if (categoryIndex === 0 && !searchTerm) {

                categoryDiv.classList.add("open");

            }


            /* ======================================
               CATEGORY HEADER
            ====================================== */

            const categoryHeader =
                document.createElement("button");

            categoryHeader.type = "button";

            categoryHeader.className = "category-header";


            categoryHeader.innerHTML = `

                <div class="category-left">

                    <div class="category-icon">

                        <i class="fa-solid fa-folder-open"></i>

                    </div>

                    <div>

                        <span class="category-name">
                            ${category.category}
                        </span>

                        <span class="category-count">
                            ${filteredModules.length}
                            ${filteredModules.length === 1
                    ? "Module"
                    : "Modules"}
                        </span>

                    </div>

                </div>

                <i class="fa-solid fa-chevron-down category-arrow"></i>

            `;


            /* ======================================
               CATEGORY CONTENT
            ====================================== */

            const categoryContent =
                document.createElement("div");

            categoryContent.className =
                "category-content";


            filteredModules.forEach(function(module) {

                const moduleCard =
                    document.createElement("a");

                moduleCard.className =
                    "module-card";


                moduleCard.href =
                    "/migration/upload/" + module.code;


                moduleCard.innerHTML = `

                    <div class="module-icon">

                        <i class="fa-solid ${module.icon}"></i>

                    </div>

                    <div class="module-details">

                        <span class="module-name">
                            ${module.name}
                        </span>

                        <span class="module-description">
                            ${module.description}
                        </span>

                        <div class="module-meta">

                            <span class="module-code">
                                ${module.code}
                            </span>

                            <span class="module-version">
                                v${module.version}
                            </span>

                        </div>

                    </div>

                    <div class="module-action">

                        <i class="fa-solid fa-arrow-right"></i>

                    </div>

                `;


                categoryContent.appendChild(moduleCard);

            });


            categoryDiv.appendChild(categoryHeader);

            categoryDiv.appendChild(categoryContent);

            accordion.appendChild(categoryDiv);


            /* ======================================
               ACCORDION CLICK
            ====================================== */

            categoryHeader.addEventListener(
                "click",
                function() {

                    const currentlyOpen =
                        categoryDiv.classList.contains("open");


                    /*
                     * Close all categories
                     */

                    accordion
                        .querySelectorAll(".module-category")
                        .forEach(function(item) {

                            item.classList.remove("open");

                        });


                    /*
                     * Open selected category
                     */

                    if (!currentlyOpen) {

                        categoryDiv.classList.add("open");

                    }

                }
            );

        });


        /* ==========================================
           EMPTY STATE
        ========================================== */

        if (visibleCategories === 0) {

            accordion.style.display = "none";

            emptyState.style.display = "block";

        } else {

            accordion.style.display = "block";

            emptyState.style.display = "none";

        }


        moduleCount.textContent = visibleModules;

    }


    /* Initial render */

    renderModules(moduleData);


    /* ==========================================
       SEARCH
    ========================================== */

    searchInput.addEventListener("input", function() {

        const value =
            searchInput.value.trim().toLowerCase();


        clearSearch.style.display =
            value ? "flex" : "none";


        renderModules(moduleData, value);

    });


    /* ==========================================
       CLEAR SEARCH
    ========================================== */

    function resetSearchBox() {

        searchInput.value = "";

        clearSearch.style.display = "none";

        renderModules(moduleData);

    }


    clearSearch.addEventListener(
        "click",
        resetSearchBox
    );


    resetSearch.addEventListener(
        "click",
        resetSearchBox
    );

});







