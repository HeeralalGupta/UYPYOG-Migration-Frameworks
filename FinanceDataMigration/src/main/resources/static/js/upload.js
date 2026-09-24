/**
 * Migration Upload
 */

document.addEventListener("DOMContentLoaded", function() {

    console.log("=================================");
    console.log("Migration Upload JS Loaded");
    console.log("=================================");
    setMigrationStep("upload");
	// ==========================================
	// SET TENANT FROM FINANCE URL
	// ==========================================

	setUserTenant();
	
	// Restore Running migration by user
	restoreRunningMigration();

    /* =====================================================
       ELEMENTS
    ===================================================== */

    const moduleCodeElement =
        document.getElementById("moduleCode");

    const fileInput =
        document.getElementById("migrationFile");

    const dropZone =
        document.getElementById("dropZone");


    const browseBtn =
        document.getElementById("browseBtn");

    const selectedFile =
        document.getElementById("selectedFile");

    const selectedFileName =
        document.getElementById("selectedFileName");

    const selectedFileSize =
        document.getElementById("selectedFileSize");

    const removeFile =
        document.getElementById("removeFile");

    const resetBtn =
        document.getElementById("resetBtn");

    const validateBtn =
        document.getElementById("validateBtn");

    const processBtn =
        document.getElementById("processBtn");

    const validationResult =
        document.getElementById("validationResult");

    const uploadStatus =
        document.getElementById("uploadStatus");

    const uploadStatusText =
        document.getElementById("uploadStatusText");

    const statusBadge =
        document.getElementById("statusBadge");


    const migrationResultPages = {

        failed: {
            records: [],
            page: 1
        },

        success: {
            records: [],
            page: 1
        },

        skipped: {
            records: [],
            page: 1
        }

    };
    const MIGRATION_PAGE_SIZE = 5;

    if (!fileInput) {

        console.error(
            "ERROR: #migrationFile not found"
        );

        return;
    }


    if (!dropZone) {

        console.error(
            "ERROR: #dropZone not found"
        );

        return;
    }


    if (!browseBtn) {

        console.error(
            "ERROR: #browseBtn not found"
        );

        return;
    }


    /* =====================================================
       MODULE CODE
    ===================================================== */

    const moduleCode =
        moduleCodeElement
            ? moduleCodeElement.value
            : "";


    console.log(
        "Migration Module:",
        moduleCode
    );


    /* =====================================================
       CONFIGURATION
    ===================================================== */

    const MAX_FILE_SIZE =
        25 * 1024 * 1024;


    const ALLOWED_EXTENSIONS =
        ["xlsx", "xls"];


    /* =====================================================
       MODULE INFORMATION
    ===================================================== */

    if (
        typeof moduleData !== "undefined"
        &&
        moduleCode
    ) {

        let moduleFound = null;


        moduleData.forEach(function(category) {

            if (
                category.modules
                &&
                Array.isArray(category.modules)
            ) {

                category.modules.forEach(
                    function(module) {

                        if (
                            module.code === moduleCode
                        ) {

                            moduleFound =
                                module;

                        }

                    }
                );

            }

        });


        if (moduleFound) {

            const moduleName =
                document.getElementById(
                    "moduleName"
                );

            const moduleDescription =
                document.getElementById(
                    "moduleDescription"
                );

            const moduleCodeDisplay =
                document.getElementById(
                    "moduleCodeDisplay"
                );

            const moduleIcon =
                document.getElementById(
                    "moduleIcon"
                );


            if (moduleName) {

                moduleName.textContent =
                    moduleFound.name;

            }


            if (moduleDescription) {

                moduleDescription.textContent =
                    moduleFound.description;

            }


            if (moduleCodeDisplay) {

                moduleCodeDisplay.textContent =
                    moduleFound.code;

            }


            if (moduleIcon) {

                moduleIcon.className =
                    "fa-solid " +
                    moduleFound.icon;

            }

        }

    }


    /* =====================================================
       BROWSE BUTTON
    ===================================================== */

    browseBtn.addEventListener(
        "click",
        function(event) {

            event.preventDefault();

            event.stopPropagation();

            console.log(
                "Browse button clicked"
            );


            fileInput.click();

        }
    );


    /* =====================================================
       DROP ZONE CLICK
    ===================================================== */

    dropZone.addEventListener(
        "click",
        function(event) {

            /*
             * Don't trigger file picker twice
             * when Browse button is clicked.
             */

            if (
                event.target === browseBtn
                ||
                browseBtn.contains(
                    event.target
                )
            ) {

                return;

            }


            console.log(
                "Drop zone clicked"
            );


            fileInput.click();

        }
    );


    /* =====================================================
       FILE SELECTED
    ===================================================== */

    fileInput.addEventListener(
        "change",
        function() {

            console.log(
                "File input changed"
            );


            if (
                fileInput.files
                &&
                fileInput.files.length > 0
            ) {

                const file =
                    fileInput.files[0];


                console.log(
                    "Selected file:",
                    file.name
                );


                handleFile(file);

            }

        }
    );


    /* =====================================================
       DRAG ENTER
    ===================================================== */

    dropZone.addEventListener(
        "dragenter",
        function(event) {

            event.preventDefault();

            event.stopPropagation();


            dropZone.classList.add(
                "drag-over"
            );

        }
    );


    /* =====================================================
       DRAG OVER
    ===================================================== */

    dropZone.addEventListener(
        "dragover",
        function(event) {

            event.preventDefault();

            event.stopPropagation();


            event.dataTransfer.dropEffect =
                "copy";


            dropZone.classList.add(
                "drag-over"
            );

        }
    );


    /* =====================================================
       DRAG LEAVE
    ===================================================== */

    dropZone.addEventListener(
        "dragleave",
        function(event) {

            event.preventDefault();

            event.stopPropagation();


            dropZone.classList.remove(
                "drag-over"
            );

        }
    );


    /* =====================================================
       DROP
    ===================================================== */

    dropZone.addEventListener(
        "drop",
        function(event) {

            event.preventDefault();

            event.stopPropagation();


            console.log(
                "File dropped"
            );


            dropZone.classList.remove(
                "drag-over"
            );


            const files =
                event.dataTransfer.files;


            if (
                files
                &&
                files.length > 0
            ) {

                const file =
                    files[0];


                console.log(
                    "Dropped file:",
                    file.name
                );


                /*
                 * Put dropped file into
                 * file input.
                 */

                try {

                    const dataTransfer =
                        new DataTransfer();


                    dataTransfer.items.add(
                        file
                    );


                    fileInput.files =
                        dataTransfer.files;

                } catch (error) {

                    console.warn(
                        "Could not assign dropped file to input",
                        error
                    );

                }


                handleFile(file);

            }

        }
    );


    /* =====================================================
       HANDLE FILE
    ===================================================== */

    function handleFile(file) {

        console.log(
            "Handling file:",
            file.name
        );


        /* ---------------------------------------------
           Extension
        --------------------------------------------- */

        const extension =
            file.name
                .split(".")
                .pop()
                .toLowerCase();


        if (
            !ALLOWED_EXTENSIONS
                .includes(extension)
        ) {

            showError(
                "Invalid File Format",
                "Please upload an XLS or XLSX file."
            );


            resetFile();

            return;

        }


        /* ---------------------------------------------
           File Size
        --------------------------------------------- */

        if (
            file.size > MAX_FILE_SIZE
        ) {

            showError(
                "File Too Large",
                "Maximum allowed file size is 25 MB."
            );


            resetFile();

            return;

        }


        /* ---------------------------------------------
           Display File
        --------------------------------------------- */

        if (selectedFileName) {

            selectedFileName.textContent =
                file.name;

        }


        if (selectedFileSize) {

            selectedFileSize.textContent =
                formatFileSize(file.size);

        }


        if (selectedFile) {

            selectedFile.style.display =
                "flex";

        }


        dropZone.style.display =
            "none";


        if (validateBtn) {

            validateBtn.disabled =
                false;

        }


        updateStatus(
            "File Ready",
            "Your file has been selected and is ready for validation.",
            "ready"
        );

        setMigrationStep("validate");

        console.log(
            "File successfully selected."
        );

    }


    /* =====================================================
       FILE SIZE
    ===================================================== */

    function formatFileSize(bytes) {

        if (bytes === 0) {

            return "0 Bytes";

        }


        const sizes = [
            "Bytes",
            "KB",
            "MB",
            "GB"
        ];


        const index =
            Math.floor(
                Math.log(bytes) /
                Math.log(1024)
            );


        return (
            parseFloat(
                (
                    bytes /
                    Math.pow(
                        1024,
                        index
                    )
                ).toFixed(2)
            )
            +
            " "
            +
            sizes[index]
        );

    }


    /* =====================================================
       REMOVE FILE
    ===================================================== */

    if (removeFile) {

        removeFile.addEventListener(
            "click",
            function(event) {

                event.preventDefault();

                event.stopPropagation();


                resetFile();

            }
        );

    }


    /* =====================================================
       RESET
    ===================================================== */

    if (resetBtn) {

        resetBtn.addEventListener(
            "click",
            function() {

                resetFile();
                window.location.reload();

            }
        );

    }


    function resetFile() {

        fileInput.value = "";


        if (selectedFile) {

            selectedFile.style.display =
                "none";

        }


        dropZone.style.display =
            "flex";

        validationResult.style.display = "none";

        processBtn.disabled = true;

        if (validateBtn) {

            validateBtn.disabled =
                true;

        }


        updateStatus(
            "Ready to Upload",
            "Select an Excel file to begin the migration process.",
            "idle"
        );

    }



    validateBtn.addEventListener(
        "click",
        async function() {

            console.log("=================================");
            console.log("VALIDATE BUTTON CLICKED");
            console.log("=================================");


            /* ---------------------------------------------
               Check file
            --------------------------------------------- */

            if (
                !fileInput.files ||
                fileInput.files.length === 0
            ) {

                console.log("NO FILE SELECTED");

                alert("Please select an Excel file first.");

                return;
            }


            const file =
                fileInput.files[0];


            console.log(
                "File:",
                file.name
            );

            console.log(
                "File size:",
                file.size
            );


            /* ---------------------------------------------
               Get module
            --------------------------------------------- */

            const moduleElement =
                document.getElementById("moduleCode");

            if (!moduleElement) {

                console.log("Module Element " + moduleElement);
                console.error(
                    "moduleCode element NOT FOUND"
                );

                alert(
                    "Module code element is missing."
                );

                return;
            }

            const tenantSelect =
                document.getElementById("tenantId");

            if (!tenantSelect) {
                console.error("Please select ulb name.");
            }


            const module =
                moduleElement.value;


            console.log(
                "Module:",
                module
            );


            /* ---------------------------------------------
               Show validating
            --------------------------------------------- */

            console.log(
                "Updating status to validating..."
            );


            updateStatus(
                "Validating File",
                "Reading Excel file and checking its structure...",
                "processing"
            );


            validateBtn.disabled = true;


            /* ---------------------------------------------
               FormData
            --------------------------------------------- */

            const formData =
                new FormData();


            formData.append(
                "file",
                file
            );


            console.log(
                "FormData created"
            );


            /* ---------------------------------------------
               API URL
            --------------------------------------------- */

            const url =
                "/migration/validate/"
                + encodeURIComponent(module);


            console.log(
                "Calling API:",
                url
            );


            try {

                console.log(
                    "Sending request..."
                );


                const response =
                    await fetch(
                        url,
                        {
                            method: "POST",
                            body: formData
                        }
                    );


                console.log(
                    "HTTP Status:",
                    response.status
                );


                console.log(
                    "HTTP OK:",
                    response.ok
                );


                const responseText =
                    await response.text();


                console.log(
                    "Raw Response:",
                    responseText
                );


                if (!response.ok) {

                    throw new Error(
                        "Server returned HTTP "
                        + response.status
                        + ": "
                        + responseText
                    );

                }


                let result;


                try {

                    result =
                        JSON.parse(responseText);

                } catch (jsonError) {

                    console.error(
                        "Invalid JSON response:",
                        jsonError
                    );

                    throw new Error(
                        "Server did not return valid JSON."
                    );

                }


                console.log(
                    "Validation Result:",
                    result
                );


                /* -----------------------------------------
                   VALID
                ----------------------------------------- */

                if (result.valid === true) {

                    console.log(
                        "VALIDATION SUCCESS",
                        result
                    );


                    updateStatus(
                        "File Valid",
                        result.totalRows +
                        " data row(s) found. Your file is ready for migration.",
                        "success"
                    );


                    showValidationSuccess(result);

                    setMigrationStep("validate");

                    if (processBtn) {

                        processBtn.disabled = false;

                    }

                }

                /* -----------------------------------------
                   INVALID
                ----------------------------------------- */
                else {

                    console.log(
                        "VALIDATION FAILED",
                        result
                    );


                    updateStatus(
                        "Validation Failed",
                        "Please correct the errors before continuing.",
                        "error"
                    );


                    /*showValidationErrors(
                        result.errors || []
                    );*/

                    showValidationErrors(result);


                    const processBtn =
                        document.getElementById(
                            "processBtn"
                        );


                    if (processBtn) {

                        processBtn.disabled = true;

                    }

                }


            } catch (error) {

                console.error(
                    "================================="
                );

                console.error(
                    "VALIDATION API ERROR"
                );

                console.error(
                    error
                );

                console.error(
                    "================================="
                );


                updateStatus(
                    "Validation Failed",
                    error.message ||
                    "Unable to validate the file.",
                    "error"
                );


            } finally {

                validateBtn.disabled =
                    false;

            }

        }
    );
	
	// ===============================
	// PROCESS BUTTON
	// ===============================

	if (processBtn) {

	    processBtn.addEventListener(
	        "click",
	        async function() {

	            console.log("====================================");
	            console.log("PROCESS MIGRATION CLICKED");
	            console.log("====================================");

	            const file = fileInput.files[0];

	            if (!file) {
	                alert(
	                    "Please select an Excel file first."
	                );
	                return;
	            }

	            const tenantId =
	                document.getElementById("tenantId").value;

	            if (!tenantId) {
	                alert(
	                    "Please select a ULB before uploading the file."
	                );
	                return;
	            }

	            const formData =
	                new FormData();

	            formData.append(
	                "file",
	                file
	            );

	            formData.append(
	                "tenantId",
	                tenantId
	            );

	            formData.append(
	                "migrationType",
	                moduleCodeElement.value
	            );
				
				const migrationUser = getMigrationUser();

				if (!migrationUser) {
				    alert("User session not found. Please login again.");
				    return;
				}

	            formData.append(
	                "uploadedBy",
	                migrationUser.username
	            );

	            console.log("File:", file.name);
	            console.log("Tenant ID:", tenantId);
	            console.log(
	                "Migration Type:",
	                moduleCodeElement.value
	            );

	            processBtn.disabled = true;

	            try {

	                const response =
	                    await fetch(
	                        getContextPath() +
	                        "/migration/process",
	                        {
	                            method: "POST",
	                            body: formData
	                        }
	                    );

	                console.log(
	                    "HTTP Status:",
	                    response.status
	                );

	                const result =
	                    await response.json();

	                console.log(
	                    "Migration Process Response:",
	                    result
	                );

	                if (!response.ok) {
	                    throw new Error(
	                        result.message ||
	                        "Migration could not be started."
	                    );
	                }

	                if (result.jobId) {

	                    startMigrationProgress(
	                        result.jobId
	                    );

	                } else {

	                    throw new Error(
	                        "Job ID was not returned by server."
	                    );
	                }

	            } catch (error) {

	                console.error(
	                    "Migration process error:",
	                    error
	                );

	                alert(
	                    error.message ||
	                    "Failed to start migration."
	                );

	                processBtn.disabled = false;
	            }
	        }
	    );
	}

	// ===============================
	// WEBSOCKET
	// ===============================

	let migrationWebSockets = new Map();


	function createMigrationProgressCard(jobId) {

	    const container =
	        document.getElementById("processProgressContainer");

	    if (!container) {
	        console.error("Progress container not found.");
	        return;
	    }

	    // ==========================================
	    // REMOVE ALL PREVIOUS MIGRATION CARDS
	    // ==========================================
	    container
	        .querySelectorAll(".migration-progress-card")
	        .forEach(function(card) {
	            card.remove();
	        });

	    // ==========================================
	    // CREATE NEW MIGRATION CARD
	    // ==========================================
	    const progressCard = `

	        <div class="migration-progress-card"
	             data-job-id="${jobId}">

	            <div class="progress-header">

	                <div class="progress-title">

	                    <div class="progress-icon">
	                        <i class="fa-solid fa-gears"></i>
	                    </div>

	                    <div>

	                        <h5>Migration in Progress</h5>

	                        <span class="processProgressMessage">
	                            Preparing migration...
	                        </span>

	                    </div>

	                </div>

	                <div class="progress-percentage processProgressPercent">
	                    0%
	                </div>

	            </div>


	            <div class="progress-wrapper">

	                <div class="progress"
	                     style="height:10px;">

	                    <div class="processProgressBar
	                                progress-bar
	                                progress-bar-striped
	                                progress-bar-animated"
	                         role="progressbar"
	                         style="width:0%;">

	                    </div>

	                </div>

	            </div>


	            <div class="migration-cancel-wrapper">

	                <button type="button"
	                        class="cancel-migration-btn"
	                        data-job-id="${jobId}">

	                    <i class="fa-solid fa-stop"></i>
	                    Cancel Migration

	                </button>

	            </div>

	        </div>

	    `;

	    container.insertAdjacentHTML(
	        "beforeend",
	        progressCard
	    );

	    container.style.display = "block";
	}



	function updateMigrationProgress(jobId, data) {

	    const card =
	        document.querySelector(
	            '.migration-progress-card[data-job-id="' +
	            jobId +
	            '"]'
	        );

	    if (!card) {

	        console.error(
	            "Progress card not found for job:",
	            jobId
	        );

	        return;
	    }


	    const progressBar =
	        card.querySelector(
	            ".processProgressBar"
	        );


	    const progressPercent =
	        card.querySelector(
	            ".processProgressPercent"
	        );


	    const progressMessage =
	        card.querySelector(
	            ".processProgressMessage"
	        );


	    const percent =
	        Number(
	            data.progressPercent ?? 0
	        );


	    // ==============================
	    // UPDATE PROGRESS BAR
	    // ==============================

	    if (progressBar) {

	        progressBar.style.width =
	            percent + "%";

	    }


	    if (progressPercent) {

	        progressPercent.innerText =
	            percent + "%";

	    }


	    // ==============================
	    // UPDATE PROGRESS MESSAGE
	    // ==============================

	    if (progressMessage) {

	        progressMessage.innerText =
	            data.currentMessage ||
	            "Migration in progress...";

	    }


	    // ==============================
	    // UPDATE SUMMARY CARDS REALTIME
	    // ==============================

	    const resultTotal =
	        document.getElementById(
	            "resultTotal"
	        );

	    const resultSuccess =
	        document.getElementById(
	            "resultSuccess"
	        );

	    const resultFailed =
	        document.getElementById(
	            "resultFailed"
	        );

	    const resultSkipped =
	        document.getElementById(
	            "resultSkipped"
	        );


	    if (resultTotal) {

	        resultTotal.innerText =
	            data.totalRecords ?? 0;

	    }


	    if (resultSuccess) {

	        resultSuccess.innerText =
	            data.successRecords ?? 0;

	    }


	    if (resultFailed) {

	        resultFailed.innerText =
	            data.failedRecords ?? 0;

	    }


	    if (resultSkipped) {

	        resultSkipped.innerText =
	            data.skippedRecords ?? 0;

	    }


	    // ==============================
	    // NORMALIZE STATUS
	    // ==============================

	    const status =
	        String(
	            data.status ?? ""
	        )
	        .trim()
	        .toUpperCase();


	    // ==============================
	    // CANCELLED
	    // ==============================

	    if (status === "CANCELLED") {

	        const title =
	            card.querySelector(
	                ".progress-title h5"
	            );


	        if (title) {

	            title.innerText =
	                "Migration Cancelled";

	        }


	        if (progressMessage) {

	            progressMessage.innerText =
	                "Migration cancelled by user.";

	        }


	        // Stop progress animation

	        if (progressBar) {

	            progressBar.classList.remove(
	                "progress-bar-animated"
	            );

	        }


	        const cancelButton =
	            card.querySelector(
	                ".cancel-migration-btn"
	            );


	        if (cancelButton) {

	            cancelButton.disabled = true;

	            cancelButton.innerHTML =
	                '<i class="fa-solid fa-circle-check"></i> Migration Cancelled';

	        }
			
			if (status === "CANCELLED") {
			    setTimeout(() => {
			        cancelButton.closest(".migration-progress-card")?.remove();
			    }, 3000);
			}


	        // Disconnect WebSocket

	        const client =
	            migrationWebSockets.get(
	                jobId
	            );


	        if (client) {

	            client.disconnect();

	            migrationWebSockets.delete(
	                jobId
	            );

	        }


	        // Load final migration result

	        loadMigrationResult(
	            jobId
	        );


	        return;
	    }


	    // ==============================
	    // COMPLETED / FAILED
	    // ==============================

	    if (
	        status === "COMPLETED" ||
	        status === "COMPLETED_WITH_ERRORS" ||
	        status === "FAILED"
	    ) {

	        const title =
	            card.querySelector(
	                ".progress-title h5"
	            );


	        if (title) {

	            title.innerText =
	                status === "FAILED"
	                    ? "Migration Failed"
	                    : "Migration Completed";

	        }


	        // Stop progress animation

	        if (progressBar) {

	            progressBar.classList.remove(
	                "progress-bar-animated"
	            );

	        }


	        // Remove cancel button

	        const cancelButton =
	            card.querySelector(
	                ".cancel-migration-btn"
	            );


	        if (cancelButton) {

	            cancelButton.remove();

	        }


	        // Disconnect WebSocket

	        const client =
	            migrationWebSockets.get(
	                jobId
	            );


	        if (client) {

	            client.disconnect();

	            migrationWebSockets.delete(
	                jobId
	            );

	        }


	        // Load final migration result

	        loadMigrationResult(
	            jobId
	        );

	    }

	}
	
	async function loadMigrationStatus(jobId) {

	    try {

	        const response = await fetch(
	            getContextPath() +
	            "/migration/progress/" +
	            encodeURIComponent(jobId)
	        );

	        if (!response.ok) {
	            return;
	        }

	        const data = await response.json();

	        updateMigrationProgress(
	            jobId,
	            data
	        );

	    } catch (error) {

	        console.error(
	            "Failed to load migration status:",
	            error
	        );
	    }
	}


	function connectMigrationWebSocket(jobId) {

	    const socket =
	        new SockJS(
	            getContextPath() + "/ws"
	        );


	    const client =
	        StompJs.Stomp.over(socket);


	    client.debug = function() {};


	    client.connect(
	        {},
	        function() {

	            console.log(
	                "WebSocket connected for job:",
	                jobId
	            );


	            // Store WebSocket against this jobId
	            migrationWebSockets.set(
	                jobId,
	                client
	            );


	            client.subscribe(
	                "/topic/migration/" + jobId,
	                function(message) {

	                    const progress =
	                        JSON.parse(
	                            message.body
	                        );


	                    updateMigrationProgress(
	                        jobId,
	                        progress
	                    );

	                }
	            );
				
				loadMigrationStatus(jobId);

	        },
	        function(error) {

	            console.error(
	                "WebSocket connection error for job:",
	                jobId,
	                error
	            );

	        }
	    );

	}



	async function cancelMigration(jobId) {

	    if (!jobId) {

	        console.error(
	            "No migration job ID."
	        );

	        return;
	    }


	    console.log(
	        "Cancelling migration:",
	        jobId
	    );


	    const confirmed =
	        confirm(
	            "Are you sure you want to cancel this migration?"
	        );


	    if (!confirmed) {
	        return;
	    }


	    const cancelButton =
	        document.querySelector(
	            '.cancel-migration-btn[data-job-id="' +
	            jobId +
	            '"]'
	        );


	    if (cancelButton) {

	        cancelButton.disabled = true;

	        cancelButton.innerHTML =
	            '<i class="fa-solid fa-spinner fa-spin"></i> Cancelling...';

	    }


	    const url =
	        getContextPath() +
	        "/migration/cancel/" +
	        encodeURIComponent(jobId);


	    try {

	        const response =
	            await fetch(
	                url,
	                {
	                    method: "POST",
	                    headers: {
	                        "Content-Type":
	                            "application/json"
	                    }
	                }
	            );


	        const result =
	            await response.json();


	        console.log(
	            "Cancellation response for job:",
	            jobId,
	            result
	        );


	        if (!response.ok) {

	            throw new Error(
	                result.message ||
	                "Failed to cancel migration."
	            );

	        }


	        if (cancelButton) {

	            cancelButton.innerHTML =
	                '<i class="fa-solid fa-spinner fa-spin"></i> Cancellation Requested...';

	        }
			
			if (result.status === "CANCELLED") {
			    setTimeout(() => {
			        cancelButton.closest(".migration-progress-card")?.remove();
			    }, 3000);
			}

	    }
	    catch (error) {

	        console.error(
	            "Failed to cancel migration:",
	            jobId,
	            error
	        );


	        if (cancelButton) {

	            cancelButton.disabled = false;

	            cancelButton.innerHTML =
	                '<i class="fa-solid fa-stop"></i> Cancel Migration';

	        }


	        alert(
	            "Failed to request migration cancellation."
	        );

	    }

	}


	// Optional global access
	window.cancelMigration =
	    cancelMigration;


	// Cancel button click
	// Works for dynamically-created cards
	document.addEventListener(
	    "click",
	    function(event) {

	        const button =
	            event.target.closest(
	                ".cancel-migration-btn"
	            );


	        if (!button) {
	            return;
	        }


	        const jobId =
	            button.getAttribute(
	                "data-job-id"
	            );


	        cancelMigration(jobId);

	    }
	);



	function startMigrationProgress(jobId) {

	    console.log(
	        "===================================="
	    );

	    console.log(
	        "STARTING MIGRATION PROGRESS"
	    );

	    console.log(
	        "Job ID :",
	        jobId
	    );

	    console.log(
	        "===================================="
	    );


	    // Create progress card for THIS job
	    createMigrationProgressCard(
	        jobId
	    );


	    setMigrationStep(
	        "process"
	    );


	    connectMigrationWebSocket(
	        jobId
	    );
		
		setTimeout(function() {

		    const card = document.querySelector(
		        '.migration-progress-card[data-job-id="' + jobId + '"]'
		    );

		    if (!card) {
		        return;
		    }

		    const progressPercent = card.querySelector(
		        ".processProgressPercent"
		    );

		    const progressMessage = card.querySelector(
		        ".processProgressMessage"
		    );

		    if (
		        progressPercent &&
		        progressMessage &&
		        progressPercent.innerText.trim() === "0%"
		    ) {

		        progressMessage.innerText =
		            "Please wait. Another migration is currently being processed. " +
		            "Your migration will start automatically once it is completed.";
		    }

		}, 5000);

	}



   
	/* =====================================================
	   RESTORE RUNNING MIGRATION BY USER, TENANT AND MODULE
	===================================================== */

	async function restoreRunningMigration() {

	    const migrationUser = getMigrationUser();

	    if (!migrationUser ||
	        !migrationUser.username ||
	        !migrationUser.tenantId) {

	        console.log("No migration user found.");
	        return;
	    }

	    const moduleCodeElement =
	        document.getElementById("moduleCode");

	    if (!moduleCodeElement || !moduleCodeElement.value) {

	        console.log("Module code not found.");
	        return;
	    }

	    const moduleCode =
	        moduleCodeElement.value;

	    try {

	        const url =
	            getContextPath() +
	            "/migration/running?username=" +
	            encodeURIComponent(migrationUser.username) +
	            "&tenantId=" +
	            encodeURIComponent(migrationUser.tenantId) +
	            "&moduleCode=" +
	            encodeURIComponent(moduleCode);

	        console.log("Checking running migration:", url);

	        const response =
	            await fetch(url);

	        if (!response.ok) {

	            console.error(
	                "Unable to find running migration. HTTP:",
	                response.status
	            );

	            return;
	        }

	        const text =
	            await response.text();

	        // Backend returned empty response
	        if (!text || !text.trim()) {

	            console.log(
	                "No running migration found."
	            );

	            return;
	        }

	        const job =
	            JSON.parse(text);

	        // No running job
	        if (!job || !job.jobId) {

	            console.log(
	                "No running migration found."
	            );

	            return;
	        }

	        console.log(
	            "Restoring running job:",
	            job.jobId
	        );

	        console.log(
	            "Migration status:",
	            job.status
	        );

	        console.log(
	            "Migration module:",
	            moduleCode
	        );

	        // 1. CREATE PROGRESS CARD

	        createMigrationProgressCard(
	            job.jobId
	        );

	        // 2. SHOW CURRENT DB PROGRESS IMMEDIATELY

	        updateMigrationProgress(
	            job.jobId,
	            job
	        );

	        // 3. CONNECT WEBSOCKET FOR REALTIME UPDATES

	        startMigrationProgress(
	            job.jobId
	        );

	    } catch (error) {

	        console.error(
	            "Failed to restore migration:",
	            error
	        );
	    }
	}
	 /* =====================================================
       VALIDATION ERRORS
    ===================================================== */

    function showValidationErrors(result) {

        const container =
            document.getElementById(
                "validationResult"
            );

        const title =
            document.getElementById(
                "validationResultTitle"
            );

        const message =
            document.getElementById(
                "validationResultMessage"
            );

        const errors =
            document.getElementById(
                "validationErrors"
            );

        const list =
            document.getElementById(
                "validationErrorList"
            );


        if (!container || !errors || !list) {

            console.error(
                "Validation result elements not found"
            );

            return;
        }


        /*
         * ---------------------------------------------
         * HEADER / DATA / COLUMN INFORMATION
         * ---------------------------------------------
         */

        const headerRow =
            document.getElementById(
                "validationHeaderRow"
            );

        const dataRows =
            document.getElementById(
                "validationDataRows"
            );

        const columnCount =
            document.getElementById(
                "validationColumnCount"
            );


        if (headerRow) {

            headerRow.textContent =
                result.headerStartRow +
                " - " +
                result.headerEndRow;
        }


        if (dataRows) {

            dataRows.textContent =
                result.totalRows || 0;
        }


        if (columnCount) {

            columnCount.textContent =
                result.columnCount || 0;
        }


        /*
         * ---------------------------------------------
         * CONTAINER
         * ---------------------------------------------
         */

        container.classList.add(
            "has-errors"
        );


        title.textContent =
            "File Validation Failed";


        message.textContent =
            "Please correct the following issues before continuing.";


        /*
         * ---------------------------------------------
         * CLEAR OLD ERRORS
         * ---------------------------------------------
         */

        list.innerHTML = "";


        /*
         * ---------------------------------------------
         * GENERAL ERRORS
         * ---------------------------------------------
         */

        if (
            result.errors &&
            result.errors.length > 0
        ) {

            result.errors.forEach(
                function(error) {

                    const li =
                        document.createElement(
                            "li"
                        );

                    li.textContent =
                        error;

                    list.appendChild(li);

                }
            );

        }


        /*
         * ---------------------------------------------
         * ROW LEVEL ERRORS
         * ---------------------------------------------
         */

        if (
            result.rowErrors &&
            result.rowErrors.length > 0
        ) {

            const rowTitle =
                document.createElement(
                    "li"
                );

            rowTitle.className =
                "validation-row-title";

            rowTitle.innerHTML = `
	            <strong>
	                <i class="fa-solid fa-table"></i>
	                Row Level Validation Errors
	            </strong>
	        `;

            list.appendChild(rowTitle);


            result.rowErrors.forEach(
                function(rowError) {

                    /*
                     * Row heading
                     */

                    const rowHeader =
                        document.createElement(
                            "li"
                        );

                    rowHeader.className =
                        "validation-row-header";

                    rowHeader.innerHTML = `
	                    <strong>
	                        <i class="fa-solid fa-file-excel"></i>
	                        Excel Row ${rowError.rowNumber}
	                    </strong>
	                `;

                    list.appendChild(
                        rowHeader
                    );


                    /*
                     * Individual errors
                     */

                    if (
                        rowError.errors &&
                        rowError.errors.length > 0
                    ) {

                        rowError.errors.forEach(
                            function(error) {

                                const li =
                                    document.createElement(
                                        "li"
                                    );

                                li.className =
                                    "validation-row-error";

                                li.innerHTML = `
	                                <i class="fa-solid fa-circle-xmark"></i>
	                                ${escapeHtml(error)}
	                            `;

                                list.appendChild(
                                    li
                                );

                            }
                        );

                    }

                }
            );

        }


        /*
         * ---------------------------------------------
         * NO ERROR DETAILS
         * ---------------------------------------------
         */

        if (
            (!result.errors ||
                result.errors.length === 0)
            &&
            (!result.rowErrors ||
                result.rowErrors.length === 0)
        ) {

            const li =
                document.createElement(
                    "li"
                );

            li.textContent =
                "Validation failed, but no detailed error message was returned.";

            list.appendChild(li);
        }


        /*
         * ---------------------------------------------
         * SHOW ERROR SECTION
         * ---------------------------------------------
         */

        errors.style.display =
            "block";

        container.style.display =
            "block";
    }
	
	/* =====================================================
	   VALIDATION SUCCESS
	===================================================== */
    function showValidationSuccess(result) {



        const container =
            document.getElementById(
                "validationResult"
            );


        const title =
            document.getElementById(
                "validationResultTitle"
            );


        const message =
            document.getElementById(
                "validationResultMessage"
            );


        const headerRow =
            document.getElementById(
                "validationHeaderRow"
            );


        const dataRows =
            document.getElementById(
                "validationDataRows"
            );


        const columnCount =
            document.getElementById(
                "validationColumnCount"
            );


        const errors =
            document.getElementById(
                "validationErrors"
            );


        container.classList.remove(
            "has-errors"
        );


        title.textContent =
            "File Validation Successful";


        message.textContent =
            result.totalRows +
            " data row(s) found. Your file is ready for migration.";


        headerRow.textContent =
            result.headerEndRow;


        dataRows.textContent =
            result.totalRows;


        columnCount.textContent =
            result.columnCount;


        errors.style.display =
            "none";


        container.style.display =
            "block";
    }

    /* =====================================================
       HTML ESCAPE
    ===================================================== */

    function escapeHtml(value) {

        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }


    /* =====================================================
       STATUS
    ===================================================== */

    function updateStatus(
        title,
        message,
        state
    ) {

        console.log(
            "STATUS:",
            title,
            message,
            state
        );


        if (uploadStatus) {

            uploadStatus.textContent =
                title;

        } else {

            console.warn(
                "#uploadStatus not found"
            );

        }


        if (uploadStatusText) {

            uploadStatusText.textContent =
                message;

        } else {

            console.warn(
                "#uploadStatusText not found"
            );

        }


        if (statusBadge) {

            statusBadge.className =
                "status-badge " + state;


            statusBadge.innerHTML = `
	            <span></span>
	            ${capitalize(state)}
	        `;

        } else {

            console.warn(
                "#statusBadge not found"
            );

        }

    }




    /* =====================================================
       CAPITALIZE
    ===================================================== */

    function capitalize(value) {

        return (
            value.charAt(0).toUpperCase()
            +
            value.slice(1)
        );

    }


    /* =====================================================
       ERROR
    ===================================================== */

    function showError(
        title,
        message
    ) {

        updateStatus(
            title,
            message,
            "error"
        );


        console.error(
            title + ": " + message
        );

    }
    function setMigrationStep(stepName) {

        console.log(
            "===================================="
        );

        console.log(
            "SETTING MIGRATION STEP :",
            stepName
        );

        console.log(
            "===================================="
        );


        const steps =
            document.querySelectorAll(
                ".migration-step"
            );

        const lines =
            document.querySelectorAll(
                ".step-line"
            );


        if (!steps || steps.length === 0) {

            console.error(
                "Migration steps not found."
            );

            return;
        }


        /*
         * Step indexes
         *
         * 0 = Upload
         * 1 = Validate
         * 2 = Process
         * 3 = Result
         */

        let activeIndex = 0;


        if (stepName === "upload") {

            activeIndex = 0;

        } else if (stepName === "validate") {

            activeIndex = 1;

        } else if (stepName === "process") {

            activeIndex = 2;

        } else if (stepName === "result") {

            activeIndex = 3;

        } else {

            console.warn(
                "Unknown migration step:",
                stepName
            );

            return;
        }


        /*
         * ==========================================
         * UPDATE STEPS
         * ==========================================
         */

        steps.forEach(
            function(step, index) {

                step.classList.remove(
                    "active"
                );

                step.classList.remove(
                    "completed"
                );


                /*
                 * Previous steps = completed
                 */

                if (index < activeIndex) {

                    step.classList.add(
                        "completed"
                    );

                }


                /*
                 * Current step = active
                 */

                if (index === activeIndex) {

                    step.classList.add(
                        "active"
                    );
                }

            }
        );



    }

    function loadMigrationResult(jobId) {

        console.log(
            "Loading migration result:",
            jobId
        );

        fetch(
			getContextPath() +
            "/migration/result/" +
            encodeURIComponent(jobId)
        )
            .then(function(response) {

                if (!response.ok) {

                    throw new Error(
                        "Unable to load migration result. HTTP " +
                        response.status
                    );

                }

                return response.json();

            })
            .then(function(records) {

                console.log(
                    "Migration result records:",
                    records
                );

                renderMigrationResult(records);

            })
            .catch(function(error) {

                console.error(
                    "Migration result error:",
                    error
                );

            });

    }

    function renderMigrationResult(records) {

        const resultSection =
            document.getElementById(
                "migrationResultSection"
            );


        if (!resultSection) {

            console.error(
                "migrationResultSection not found"
            );

            return;

        }


        /*
         * =====================================================
         * SAFETY CHECK
         * =====================================================
         */

        if (!Array.isArray(records)) {

            console.error(
                "Invalid migration result records:",
                records
            );

            records = [];

        }


        /*
         * =====================================================
         * SEPARATE RECORDS BY STATUS
         * =====================================================
         */

        const successRecords =
            records.filter(function(record) {

                return String(
                    record.status || ""
                )
                    .trim()
                    .toUpperCase() === "SUCCESS";

            });


        const failedRecords =
            records.filter(function(record) {

                return String(
                    record.status || ""
                )
                    .trim()
                    .toUpperCase() === "FAILED";

            });


        const skippedRecords =
            records.filter(function(record) {

                return String(
                    record.status || ""
                )
                    .trim()
                    .toUpperCase() === "SKIPPED";

            });


        /*
         * =====================================================
         * COUNTS
         * =====================================================
         */

        const successCount =
            successRecords.length;

        const failedCount =
            failedRecords.length;

        const skippedCount =
            skippedRecords.length;


        /*
         * =====================================================
         * STORE RECORDS
         *
         * IMPORTANT:
         * We don't directly insert all records into tbody.
         * Pagination will decide which 10 records to display.
         * =====================================================
         */

        migrationResultPages.success.records =
            successRecords;

        migrationResultPages.success.page =
            1;


        migrationResultPages.failed.records =
            failedRecords;

        migrationResultPages.failed.page =
            1;


        migrationResultPages.skipped.records =
            skippedRecords;

        migrationResultPages.skipped.page =
            1;


        /*
         * =====================================================
         * UPDATE SUMMARY COUNTERS
         * =====================================================
         */

        const resultTotal =
            document.getElementById(
                "resultTotal"
            );

        const resultSuccess =
            document.getElementById(
                "resultSuccess"
            );

        const resultFailed =
            document.getElementById(
                "resultFailed"
            );

        const resultSkipped =
            document.getElementById(
                "resultSkipped"
            );


        if (resultTotal) {

            resultTotal.innerText =
                records.length;

        }


        if (resultSuccess) {

            resultSuccess.innerText =
                successCount;

        }


        if (resultFailed) {

            resultFailed.innerText =
                failedCount;

        }


        if (resultSkipped) {

            resultSkipped.innerText =
                skippedCount;

        }


        /*
         * =====================================================
         * UPDATE BADGES
         * =====================================================
         */

        const failedBadge =
            document.getElementById(
                "failedCountBadge"
            );

        const successBadge =
            document.getElementById(
                "successCountBadge"
            );

        const skippedBadge =
            document.getElementById(
                "skippedCountBadge"
            );


        if (failedBadge) {

            failedBadge.innerText =
                failedCount;

        }


        if (successBadge) {

            successBadge.innerText =
                successCount;

        }


        if (skippedBadge) {

            skippedBadge.innerText =
                skippedCount;

        }


        /*
         * =====================================================
         * SHOW / HIDE RESULT TABLES
         * =====================================================
         */

        const failedSection =
            document.getElementById(
                "failedRecordsSection"
            );

        const successSection =
            document.getElementById(
                "successRecordsSection"
            );

        const skippedSection =
            document.getElementById(
                "skippedRecordsSection"
            );


        if (failedSection) {

            failedSection.style.display =
                failedCount > 0
                    ? "block"
                    : "none";

        }


        if (successSection) {

            successSection.style.display =
                successCount > 0
                    ? "block"
                    : "none";

        }


        if (skippedSection) {

            skippedSection.style.display =
                skippedCount > 0
                    ? "block"
                    : "none";

        }


        /*
         * =====================================================
         * RENDER FIRST PAGE OF EACH TABLE
         * =====================================================
         */

        renderMigrationPage(
            "failed"
        );


        renderMigrationPage(
            "success"
        );


        renderMigrationPage(
            "skipped"
        );


        /*
         * =====================================================
         * RESULT SUMMARY
         * =====================================================
         */

        const summary =
            document.getElementById(
                "resultSummary"
            );


        if (summary) {

            if (failedCount > 0) {

                summary.innerText =
                    "Migration completed with " +
                    failedCount +
                    " failed record(s).";

            }

            else if (skippedCount > 0) {

                summary.innerText =
                    "Migration completed successfully. " +
                    skippedCount +
                    " record(s) were skipped as duplicates.";

            }

            else {

                summary.innerText =
                    "All migration records processed successfully.";

            }

        }


        /*
         * =====================================================
         * STATUS BADGE
         * =====================================================
         */

        const statusBadge =
            document.getElementById(
                "resultStatusBadge"
            );


        if (statusBadge) {

            if (failedCount > 0) {

                statusBadge.innerHTML =
                    '<i class="fa-solid fa-triangle-exclamation"></i> ' +
                    'Completed With Errors';


                statusBadge.style.background =
                    "#fff3cd";


                statusBadge.style.color =
                    "#9a6700";

            }

            else {

                statusBadge.innerHTML =
                    '<i class="fa-solid fa-circle-check"></i> ' +
                    'Completed';


                statusBadge.style.background =
                    "#e9f9ef";


                statusBadge.style.color =
                    "#138a47";

            }

        }


        /*
         * =====================================================
         * SHOW RESULT SECTION
         * =====================================================
         */

        resultSection.style.display =
            "block";


        /*
         * =====================================================
         * SCROLL TO RESULT
         * =====================================================
         */

        setTimeout(function() {

            resultSection.scrollIntoView({

                behavior: "smooth",

                block: "start"

            });

        }, 100);

    }

    function renderMigrationPage(type) {

        console.log("====================================");
        console.log("RENDER MIGRATION PAGE");
        console.log("Type :", type);
        console.log("====================================");

        const config = migrationResultPages[type];

        if (!config) {

            console.error(
                "Pagination config not found:",
                type
            );

            return;
        }


        const records = config.records || [];

        const totalRecords = records.length;

        const pageSize = MIGRATION_PAGE_SIZE;

        const totalPages =
            Math.max(
                1,
                Math.ceil(totalRecords / pageSize)
            );


        /*
         * Safety
         */

        if (config.page < 1) {
            config.page = 1;
        }

        if (config.page > totalPages) {
            config.page = totalPages;
        }


        /*
         * =====================================================
         * CURRENT PAGE DATA
         * =====================================================
         */

        const startIndex =
            (config.page - 1) * pageSize;

        const endIndex =
            Math.min(
                startIndex + pageSize,
                totalRecords
            );


        const pageRecords =
            records.slice(
                startIndex,
                endIndex
            );


        console.log(
            "Page :",
            config.page,
            "/",
            totalPages
        );

        console.log(
            "Records :",
            startIndex,
            "-",
            endIndex
        );


        /*
         * =====================================================
         * TABLE BODY
         * =====================================================
         */

        let bodyId = "";

        if (type === "failed") {

            bodyId = "failedRecordsBody";

        } else if (type === "success") {

            bodyId = "successRecordsBody";

        } else if (type === "skipped") {

            bodyId = "skippedRecordsBody";

        }


        const tbody =
            document.getElementById(bodyId);


        if (!tbody) {

            console.error(
                "Table body not found:",
                bodyId
            );

            return;
        }


        tbody.innerHTML = "";


        /*
         * =====================================================
         * RENDER CURRENT PAGE
         * =====================================================
         *
         * IMPORTANT:
         *
         * S.NO. is independent for each table.
         *
         * Failed:
         * 1, 2, 3...
         *
         * Success:
         * 1, 2, 3...
         *
         * Skipped:
         * 1, 2, 3...
         *
         */

        pageRecords.forEach(
            function(record, index) {

                const serialNumber =
                    startIndex + index + 1;


                tbody.innerHTML +=
                    createResultRow(
                        record,
                        type,
                        serialNumber
                    );

            }
        );


        /*
         * =====================================================
         * PAGINATION INFO
         * =====================================================
         */

        const info =
            document.getElementById(
                type + "PaginationInfo"
            );


        if (info) {

            if (totalRecords === 0) {

                info.innerText =
                    "No records";

            } else {

                info.innerText =
                    "Showing " +
                    (startIndex + 1) +
                    "-" +
                    endIndex +
                    " of " +
                    totalRecords;

            }

        }


        /*
         * =====================================================
         * PREVIOUS BUTTON
         * =====================================================
         */

        const previousButton =
            document.getElementById(
                type + "PreviousPage"
            );


        if (previousButton) {

            previousButton.disabled =
                config.page <= 1;

            previousButton.classList.toggle(
                "disabled",
                config.page <= 1
            );

        }


        /*
         * =====================================================
         * NEXT BUTTON
         * =====================================================
         */

        const nextButton =
            document.getElementById(
                type + "NextPage"
            );


        if (nextButton) {

            nextButton.disabled =
                config.page >= totalPages;

            nextButton.classList.toggle(
                "disabled",
                config.page >= totalPages
            );

        }


        /*
         * =====================================================
         * PAGE NUMBERS
         * =====================================================
         */

        const pageNumbers =
            document.getElementById(
                type + "PageNumbers"
            );


        if (pageNumbers) {

            pageNumbers.innerHTML = "";


            for (
                let page = 1;
                page <= totalPages;
                page++
            ) {

                const button =
                    document.createElement(
                        "button"
                    );


                button.type = "button";


                button.className =
                    "migration-page-btn";


                if (page === config.page) {

                    button.classList.add(
                        "active"
                    );

                }


                button.innerText = page;


                /*
                 * IMPORTANT:
                 * Do NOT use inline onclick.
                 */

                button.addEventListener(
                    "click",
                    function() {

                        config.page = page;

                        renderMigrationPage(
                            type
                        );

                    }
                );


                pageNumbers.appendChild(
                    button
                );

            }

        }

    }

    function changeMigrationPage(type, direction) {

        console.log("====================================");
        console.log("PAGINATION CLICK");
        console.log("Type :", type);
        console.log("Direction :", direction);
        console.log("====================================");


        const config =
            migrationResultPages[type];


        if (!config) {

            console.error(
                "Pagination config not found:",
                type
            );

            return;

        }


        const totalRecords =
            config.records.length;


        const totalPages =
            Math.max(
                1,
                Math.ceil(
                    totalRecords /
                    MIGRATION_PAGE_SIZE
                )
            );


        const newPage =
            config.page + direction;


        /*
         * Don't go outside page range
         */

        if (
            newPage < 1 ||
            newPage > totalPages
        ) {

            console.log(
                "Pagination boundary reached."
            );

            return;

        }


        config.page =
            newPage;


        renderMigrationPage(
            type
        );

    }

    function updateMigrationPagination(
        type,
        totalRecords,
        currentPage,
        totalPages,
        startIndex,
        endIndex
    ) {

        let prefix;

        if (type === "failed") {

            prefix = "failed";

        } else if (type === "success") {

            prefix = "success";

        } else if (type === "skipped") {

            prefix = "skipped";

        } else {

            return;

        }


        /*
         * =====================================================
         * FIND PAGINATION ELEMENTS
         * =====================================================
         */

        const info =
            document.getElementById(
                prefix + "PaginationInfo"
            );


        const prevButton =
            document.getElementById(
                prefix + "PrevButton"
            );


        const nextButton =
            document.getElementById(
                prefix + "NextButton"
            );


        const pageContainer =
            document.getElementById(
                prefix + "PaginationPages"
            );


        /*
         * =====================================================
         * SHOWING INFORMATION
         * =====================================================
         */

        if (info) {

            if (totalRecords === 0) {

                info.innerText =
                    "No records";

            } else {

                info.innerText =
                    "Showing " +
                    (startIndex + 1) +
                    "-" +
                    endIndex +
                    " of " +
                    totalRecords;

            }

        }


        /*
         * =====================================================
         * PREVIOUS BUTTON
         * =====================================================
         */

        if (prevButton) {

            prevButton.disabled =
                currentPage <= 1;

        }


        /*
         * =====================================================
         * NEXT BUTTON
         * ===================================================== */

        if (nextButton) {

            nextButton.disabled =
                currentPage >= totalPages;

        }


        /*
         * =====================================================
         * PAGE NUMBERS
         * =====================================================
         */

        if (pageContainer) {

            pageContainer.innerHTML = "";


            if (totalRecords === 0) {

                return;

            }


            for (
                let page = 1;
                page <= totalPages;
                page++
            ) {

                const button =
                    document.createElement("button");


                button.type = "button";


                button.className =
                    "migration-page-btn" +
                    (
                        page === currentPage
                            ? " active"
                            : ""
                    );


                button.innerText =
                    page;


                button.addEventListener(
                    "click",
                    function() {

                        changeMigrationPage(
                            type,
                            page
                        );

                    }
                );


                pageContainer.appendChild(
                    button
                );

            }

        }

    }

    function createResultRow(
        record,
        type,
        serialNumber
    ) {

        const startRow =
            record.startRow || "-";


        const endRow =
            record.endRow || "-";


        const excelRows =
            startRow + " - " + endRow;


        const executionTime =
            record.executionTime != null
                ? record.executionTime + " ms"
                : "0 ms";


        const message =
            record.message ||
            "No message available.";

        let displayError = message;

        const messageMatch = message.match(
            /"message"\s*:\s*"([^"]+)"/i
        );

        if (messageMatch && messageMatch[1]) {
            displayError = messageMatch[1];
        }


        /*
         * =====================================================
         * FAILED
         * =====================================================
         */

        if (type === "failed") {

            return `
	            <tr class="result-row-failed">

	                <td>
	                    <strong>
	                        ${serialNumber}
	                    </strong>
	                </td>

	                <td>
	                    <strong>
	                        ${excelRows}
	                    </strong>
	                </td>

	                <td>
	                    <span class="result-status failed">
	                        <i class="fa-solid fa-xmark"></i>
	                        FAILED
	                    </span>
	                </td>

	                <td class="result-message failed-message">

	                    <i class="fa-solid fa-circle-exclamation"></i>

	                    <span>
	                        ${escapeHtml(getDisplayError(displayError, "Please check the Bank and try again."))}
	                    </span>

	                </td>

	                <td>
	                    ${executionTime}
	                </td>

	            </tr>
	        `;

        }
		function getDisplayError(displayError, bankName) {

		    if (!displayError) {
		        return "";
		    }

		    var error = displayError.toLowerCase();

		    if (error.includes("arjuna016053")
		            || error.includes("jta transaction unexpectedly rolled back")
		            || error.includes("could not commit transaction")) {

		        return "Bank already exists: " + bankName;
		    }

		    return displayError;
		}

        /*
         * =====================================================
         * SUCCESS
         * =====================================================
         */

        if (type === "success") {

            return `
	            <tr class="result-row-success">

	                <td>
	                    <strong>
	                        ${serialNumber}
	                    </strong>
	                </td>

	                <td>
	                    <strong>
	                        ${excelRows}
	                    </strong>
	                </td>

	                <td>
	                    <span class="result-status success">
	                        <i class="fa-solid fa-check"></i>
	                        SUCCESS
	                    </span>
	                </td>

	                <td class="result-message success-message">

	                    <i class="fa-solid fa-circle-check"></i>

	                    <span>
	                        ${escapeHtml(message)}
	                    </span>

	                </td>

	                <td>
	                    ${executionTime}
	                </td>

	            </tr>
	        `;

        }


        /*
         * =====================================================
         * SKIPPED
         * =====================================================
         */

        if (type === "skipped") {

            return `
	            <tr class="result-row-skipped">

	                <td>
	                    <strong>
	                        ${serialNumber}
	                    </strong>
	                </td>

	                <td>
	                    <strong>
	                        ${excelRows}
	                    </strong>
	                </td>

	                <td>
	                    <span class="result-status skipped">
	                        <i class="fa-solid fa-forward"></i>
	                        SKIPPED
	                    </span>
	                </td>

	                <td class="result-message skipped-message">

	                    <i class="fa-solid fa-forward"></i>

	                    <span>
	                        ${escapeHtml(message)}
	                    </span>

	                </td>

	                <td>
	                    ${executionTime}
	                </td>

	            </tr>
	        `;

        }


        return "";

    }

    function escapeHtml(value) {

        if (value === null || value === undefined) {

            return "";

        }


        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");

    }

    console.log("jQuery:", typeof jQuery);
    console.log("Select2:", typeof $.fn.select2);

    $("#tenantId").select2({

        placeholder: "Search & Select ULB",

        allowClear: true,

        width: "100%",

        minimumResultsForSearch: 0

    });




// DOM Ended
});


// Setting user tenants
function setUserTenant() {

    const params = new URLSearchParams(window.location.search);

    const tenantId = params.get("ms_tenant_id");
    const username = params.get("username");

    console.log("URL Tenant:", tenantId);
    console.log("URL Username:", username);

    const tenantSelect =
        document.getElementById("tenantId");

    if (!tenantSelect) {
        return;
    }

    if (!tenantId) {

        console.error(
            "Tenant ID was not provided in URL."
        );

        tenantSelect.innerHTML =
            '<option value="">Tenant not provided</option>';

        tenantSelect.disabled = true;

        return;
    }

    /*
     * Show ONLY the tenant received from URL
     */
    const displayName =
        tenantId.startsWith("hr.")
            ? tenantId.substring(3)
            : tenantId;

    tenantSelect.innerHTML = `
					        <option value="${tenantId}" selected>
					            ${displayName.charAt(0).toUpperCase() + displayName.slice(1)}
					        </option>
					    `;

    /*
     * Lock tenant selection
     */
    tenantSelect.disabled = true;

    console.log("Selected Tenant:", tenantSelect.value);
}
