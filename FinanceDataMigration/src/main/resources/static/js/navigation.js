/**
 * 
 */
document.addEventListener("DOMContentLoaded", function () {

    const current = window.location.pathname;

    document.querySelectorAll(".nav-item").forEach(item => {

        item.classList.remove("active");

        if(current === item.getAttribute("href")){

            item.classList.add("active");

        }

    });


});

