/**
 * Migration Upload
 */

document.addEventListener("DOMContentLoaded", function() {

    console.log("=================================");
    console.log("Migration Upload JS Loaded");
    console.log("=================================");
    setMigrationStep("upload");

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

                    const processBtn =
                        document.getElementById(
                            "processBtn"
                        );

                    processBtn.addEventListener("click", async function() {

                        console.log("====================================");
                        console.log("PROCESS MIGRATION CLICKED");
                        console.log("====================================");

                        if (!fileInput.files.length) {

                            console.error("No file selected.");
                            return;
                        }

                        const file = fileInput.files[0];

                        const formData = new FormData();

                        formData.append(
                            "file",
                            file
                        );

                        formData.append(
                            "tenantId",
                            "hr.gurugram"
                        );

                        formData.append(
                            "migrationType",
                            moduleCode
                        );

                        formData.append(
                            "uploadedBy",
                            "ADMIN"
                        );

                        console.log(
                            "File:",
                            file.name
                        );

                        console.log(
                            "Module:",
                            moduleCode
                        );

                        try {

                            processBtn.disabled = true;

                            processBtn.innerHTML = `
						            <i class="fa-solid fa-spinner fa-spin"></i>
						            Processing...
						        `;


                            // Move Process step to active
                            setMigrationStep("process");


                            const response = await fetch(
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
                                "Migration Start Response:",
                                result
                            );


                            if (!response.ok) {

                                throw new Error(
                                    result.message ||
                                    "Migration could not be started."
                                );
                            }


                            /*
                             * ==========================================
                             * START REAL-TIME PROGRESS
                             * ==========================================
                             */

                            if (result.jobId) {

                                console.log(
                                    "Starting progress polling for job:",
                                    result.jobId
                                );

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
                                "Migration failed:",
                                error
                            );

                            processBtn.disabled = false;

                            processBtn.innerHTML = `
						            <i class="fa-solid fa-play"></i>
						            Process Migration
						        `;

                        }

                    });
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

    function startMigrationProgress(jobId) {

        console.log("====================================");
        console.log("STARTING MIGRATION PROGRESS");
        console.log("Job ID :", jobId);
        console.log("====================================");

        const progressContainer =
            document.getElementById("processProgressContainer");

        const progressBar =
            document.getElementById("processProgressBar");

        const progressPercent =
            document.getElementById("processProgressPercent");

        const progressMessage =
            document.getElementById("processProgressMessage");

        const progressTotal =
            document.getElementById("progressTotal");

        const progressSuccess =
            document.getElementById("progressSuccess");

        const progressSkipped =
            document.getElementById("progressSkipped");

        const progressFailed =
            document.getElementById("progressFailed");

        const processBtn =
            document.getElementById("processBtn");


        if (progressContainer) {
            progressContainer.style.display = "block";
        }


        /*
         * Process step
         */
        setMigrationStep("process");


        /*
         * Poll every second
         */
        const interval = setInterval(function() {

            fetch(
                "/migration/progress/" +
                encodeURIComponent(jobId)
            )
                .then(function(response) {

                    if (!response.ok) {

                        throw new Error(
                            "Progress API returned HTTP " +
                            response.status
                        );
                    }

                    return response.json();
                })
                .then(function(data) {

                    console.log(
                        "Migration Progress :",
                        data
                    );


                    /*
                     * ==========================================
                     * READ VALUES
                     * ==========================================
                     */

                    const total =
                        Number(data.totalRecords ?? 0);

                    const success =
                        Number(data.successRecords ?? 0);

                    const failed =
                        Number(data.failedRecords ?? 0);

                    const skipped =
                        Number(data.skippedRecords ?? 0);

                    const current =
                        Number(data.currentRecord ?? 0);

                    const percent =
                        Number(data.progressPercent ?? 0);

                    const status =
                        String(data.status ?? "")
                            .trim()
                            .toUpperCase();


                    console.log(
                        "------------------------------------"
                    );

                    console.log(
                        "TOTAL   :", total
                    );

                    console.log(
                        "CURRENT :", current
                    );

                    console.log(
                        "SUCCESS :", success
                    );

                    console.log(
                        "FAILED  :", failed
                    );

                    console.log(
                        "SKIPPED :", skipped
                    );

                    console.log(
                        "PERCENT :", percent
                    );

                    console.log(
                        "STATUS  :", status
                    );

                    console.log(
                        "------------------------------------"
                    );


                    /*
                     * ==========================================
                     * UPDATE PROGRESS BAR
                     * ==========================================
                     */

                    if (progressBar) {

                        progressBar.style.width =
                            percent + "%";
                    }


                    if (progressPercent) {

                        progressPercent.innerText =
                            percent + "%";
                    }


                    /*
                     * ==========================================
                     * UPDATE MESSAGE
                     * ==========================================
                     */

                    if (progressMessage) {

                        progressMessage.innerText =
                            data.currentMessage ||
                            "Migration in progress...";
                    }


                    /*
                     * ==========================================
                     * UPDATE COUNTERS
                     * ==========================================
                     */

                    if (progressTotal) {

                        progressTotal.innerText =
                            String(total);
                    }

                    if (progressSuccess) {

                        progressSuccess.innerText =
                            String(success);
                    }

                    if (progressSkipped) {

                        progressSkipped.innerText =
                            String(skipped);
                    }

                    if (progressFailed) {

                        progressFailed.innerText =
                            String(failed);
                    }


                    /*
                     * ==========================================
                     * FINAL STATUS
                     * ==========================================
                     */

                    if (
                        status === "COMPLETED" ||
                        status === "COMPLETED_WITH_ERRORS" ||
                        status === "FAILED"
                    ) {

                        console.log(
                            "===================================="
                        );

                        console.log(
                            "MIGRATION COMPLETED"
                        );

                        console.log(
                            "FINAL STATUS :",
                            status
                        );

                        console.log(
                            "FINAL TOTAL :",
                            total
                        );

                        console.log(
                            "FINAL SUCCESS :",
                            success
                        );

                        console.log(
                            "FINAL FAILED :",
                            failed
                        );

                        console.log(
                            "FINAL SKIPPED :",
                            skipped
                        );

                        console.log(
                            "===================================="
                        );


                        /*
                         * STOP POLLING
                         */

                        clearInterval(interval);


                        /*
                         * ======================================
                         * FORCE FINAL VALUES
                         * ======================================
                         */

                        if (progressBar) {

                            progressBar.style.width =
                                "100%";

                            progressBar.classList.remove(
                                "progress-bar-striped"
                            );

                            progressBar.classList.remove(
                                "progress-bar-animated"
                            );
                        }


                        if (progressPercent) {

                            progressPercent.innerText =
                                "100%";
                        }


                        /*
                         * IMPORTANT:
                         * Set counters AGAIN after final status.
                         */

                        if (progressTotal) {

                            progressTotal.innerText =
                                String(total);
                        }

                        if (progressSuccess) {

                            progressSuccess.innerText =
                                String(success);
                        }

                        if (progressSkipped) {

                            progressSkipped.innerText =
                                String(skipped);
                        }

                        if (progressFailed) {

                            progressFailed.innerText =
                                String(failed);
                        }


                        /*
                         * ======================================
                         * FINAL MESSAGE
                         * ======================================
                         */

                        if (progressMessage) {

                            if (
                                status ===
                                "COMPLETED_WITH_ERRORS"
                            ) {

                                progressMessage.innerText =
                                    "Migration completed with " +
                                    failed +
                                    " failed record(s).";

                            } else if (
                                status === "COMPLETED"
                            ) {

                                progressMessage.innerText =
                                    "Migration completed successfully.";

                            } else {

                                progressMessage.innerText =
                                    "Migration failed.";
                            }
                        }


                        /*
                         * ======================================
                         * MOVE PROCESS → RESULT
                         * ======================================
                         */

                        console.log(
                            "Moving Process step to Result..."
                        );

                        setMigrationStep("result");

                        loadMigrationResult(jobId);
                        /*
                         * ======================================
                         * UPDATE PROCESS BUTTON
                         * ======================================
                         */

                        if (processBtn) {

                            processBtn.disabled = false;

                            processBtn.classList.remove(
                                "disabled"
                            );

                            processBtn.removeAttribute(
                                "disabled"
                            );


                            if (
                                status ===
                                "COMPLETED"
                            ) {

                                processBtn.innerHTML =
                                    '<i class="fa-solid fa-check"></i> ' +
                                    'Migration Completed';

                            } else if (
                                status ===
                                "COMPLETED_WITH_ERRORS"
                            ) {

                                processBtn.innerHTML =
                                    '<i class="fa-solid fa-triangle-exclamation"></i> ' +
                                    'Completed With Errors';

                            } else {

                                processBtn.innerHTML =
                                    '<i class="fa-solid fa-xmark"></i> ' +
                                    'Migration Failed';
                            }
                        }


                        /*
                         * ======================================
                         * RESULT TABLE
                         * ======================================
                         *
                         * Do NOT call showMigrationResult()
                         * here yet.
                         *
                         * It may be overwriting the progress UI.
                         *
                         */

                    }

                })
                .catch(function(error) {

                    console.error(
                        "Migration progress error:",
                        error
                    );

                });

        }, 1000);
    }

    /* =====================================================
       VALIDATION SUCCESS
    ===================================================== */

    function showValidationSuccess(result) {

        const container =
            document.getElementById(
                "validationResult"
            );

        if (!container) {
            return;
        }

        container.innerHTML = `

	        <div class="validation-success">

	            <div class="validation-icon">

	                <i class="fa-solid fa-circle-check"></i>

	            </div>

	            <div class="validation-content">

	                <strong>
	                    File validation successful
	                </strong>

	                <span>
	                    ${result.totalRows}
	                    data row(s) found.
	                    Your file is ready for migration.
	                </span>

	            </div>

	        </div>

	    `;

        container.style.display =
            "block";
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
	                        ${escapeHtml(displayError)}
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


});