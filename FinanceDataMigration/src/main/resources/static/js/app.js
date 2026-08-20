/**
 * 
 */
function updateDateTime(){

    const now = new Date();

    document.getElementById("currentDate").innerHTML =
        now.toLocaleString();

}

updateDateTime();

setInterval(updateDateTime,1000);