<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<nav class="migration-navbar">

    <div class="nav-left">

        <!-- HOME -->
        <a href="<c:url value='/migration'/>"
           class="nav-item"
           data-page="home">

            <i class="fa-solid fa-house"></i>

            <span>Home</span>

        </a>


        <!-- MIGRATION -->
        <a href="<c:url value='/migration/new'/>"
           class="nav-item"
           data-page="migration">

            <i class="fa-solid fa-cloud-arrow-up"></i>

            <span>Migration</span>

        </a>


        <!-- HISTORY -->
        <a href="<c:url value='/migration/history'/>"
           class="nav-item"
           data-page="history">

            <i class="fa-solid fa-clock-rotate-left"></i>

            <span>History</span>

        </a>


        <!-- REPORTS -->
        <a href="<c:url value='/migration/reports'/>"
           class="nav-item"
           data-page="reports">

            <i class="fa-solid fa-chart-column"></i>

            <span>Reports</span>

        </a>


        <!-- SETTINGS -->
        <a href="<c:url value='/migration/settings'/>"
           class="nav-item"
           data-page="settings">

            <i class="fa-solid fa-gear"></i>

            <span>Settings</span>

        </a>

    </div>


    <div class="nav-right">

        <button type="button"
                class="notification-btn"
                title="Notifications">

            <i class="fa-solid fa-bell"></i>

            <span class="notification-dot"></span>

        </button>


        <button type="button"
                class="help-btn">

            <i class="fa-solid fa-circle-question"></i>

            <span>Help</span>

        </button>

    </div>

</nav>


<script>

document.addEventListener("DOMContentLoaded", function () {

    const currentPath = window.location.pathname;

    const navItems =
        document.querySelectorAll(".migration-navbar .nav-item");


    navItems.forEach(function (item) {

        const page =
            item.getAttribute("data-page");


        item.classList.remove("active");


        /*
         * HOME
         */
        if (
            page === "home"
            &&
            (
                currentPath === "/migration"
                ||
                currentPath === "/migration/"
            )
        ) {

            item.classList.add("active");

        }


        /*
         * MIGRATION
         *
         * Both module selection and upload
         * belong to Migration.
         */
        else if (
            page === "migration"
            &&
            (
                currentPath === "/migration/new"
                ||
                currentPath.startsWith("/migration/upload/")
            )
        ) {

            item.classList.add("active");

        }


        /*
         * HISTORY
         */
        else if (
            page === "history"
            &&
            currentPath.startsWith("/migration/history")
        ) {

            item.classList.add("active");

        }


        /*
         * REPORTS
         */
        else if (
            page === "reports"
            &&
            currentPath.startsWith("/migration/reports")
        ) {

            item.classList.add("active");

        }


        /*
         * SETTINGS
         */
        else if (
            page === "settings"
            &&
            currentPath.startsWith("/migration/settings")
        ) {

            item.classList.add("active");

        }

    });

});

</script>