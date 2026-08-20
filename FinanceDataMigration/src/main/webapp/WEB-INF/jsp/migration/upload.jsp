<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Migration Upload</title>
<%@ include file="../layout/head.jsp"%>
</head>

<body>

	<!-- HEADER -->

	<%@ include file="../layout/header.jsp"%>


	<!-- NAVBAR -->

	<%@ include file="../layout/navbar.jsp"%>


	<main class="migration-upload-page">


		<!-- =================================================
             PAGE HEADER
        ================================================== -->

		<section class="upload-page-header">


			<div class="upload-heading">


				<a href="<c:url value='/migration/new'/>" class="upload-back-btn">

					<i class="fa-solid fa-arrow-left"></i>

				</a>


				<div class="upload-module-icon">

					<i id="moduleIcon" class="fa-solid fa-cloud-arrow-up"></i>

				</div>


				<div>

					<div class="upload-eyebrow">

						<i class="fa-solid fa-layer-group"></i> MIGRATION WORKSPACE

					</div>


					<h1 id="moduleName">Migration</h1>


					<p id="moduleDescription">Upload and migrate your data
						securely.</p>

				</div>

			</div>


			<div class="module-code-display">

				<span>MODULE</span> <strong id="moduleCodeDisplay">
					${moduleCode} </strong>

			</div>


		</section>



		<!-- =================================================
             PROGRESS STEPS
        ================================================== -->

		<section class="migration-steps-card">


			<div class="migration-step active">

				<div class="step-circle">

					<i class="fa-solid fa-upload"></i>

				</div>

				<div class="step-content">

					<strong>Upload</strong> <span>Select file</span>

				</div>

			</div>


			<div class="step-line"></div>


			<div class="migration-step">

				<div class="step-circle">

					<i class="fa-solid fa-check-double"></i>

				</div>

				<div class="step-content">

					<strong>Validate</strong> <span>Check data</span>

				</div>

			</div>


			<div class="step-line"></div>


			<div class="migration-step">

				<div class="step-circle">

					<i class="fa-solid fa-gears"></i>

				</div>

				<div class="step-content">

					<strong>Process</strong> <span>Create records</span>

				</div>

			</div>


			<div class="step-line"></div>


			<div class="migration-step">

				<div class="step-circle">

					<i class="fa-solid fa-flag-checkered"></i>

				</div>

				<div class="step-content">

					<strong>Result</strong> <span>View summary</span>

				</div>

			</div>


		</section>

		<div id="processProgressContainer" class="migration-progress-card"
			style="display: none;">

			<div class="progress-header">

				<div class="progress-title">

					<div class="progress-icon">
						<i class="fa-solid fa-gears"></i>
					</div>

					<div>
						<h5>Migration in Progress</h5>

						<span id="processProgressMessage"> Preparing migration... </span>
					</div>

				</div>


				<div class="progress-percentage" id="processProgressPercent">

					0%</div>

			</div>


			<div class="progress-wrapper">

				<div class="progress" style="height: 10px;">

					<div id="processProgressBar"
						class="progress-bar progress-bar-striped progress-bar-animated"
						role="progressbar" style="width: 0%;"></div>

				</div>

			</div>

		</div>

		<div class="upload-content-grid">


			<!-- =================================================
                 UPLOAD CARD
            ================================================== -->

			<section class="upload-card">


				<div class="card-header">


					<div class="card-title">

						<div class="card-title-icon blue">

							<i class="fa-solid fa-file-arrow-up"></i>

						</div>


						<div>

							<h3>Upload Migration File</h3>

							<p>Select an Excel file containing your migration data</p>

						</div>

					</div>


					<span class="required-badge"> <i
						class="fa-solid fa-asterisk"></i> Required

					</span>

				</div>



				<!-- DROP ZONE -->

				<div id="dropZone" class="upload-drop-zone">


					<input type="file" id="migrationFile" name="file"
						accept=".xls,.xlsx" style="display: none;"> <input
						type="hidden" id="moduleCode" value="${moduleCode}">

					<div class="drop-icon">

						<i class="fa-solid fa-file-excel"></i>

					</div>

					<h3>Drag & Drop your Excel file here</h3>
					<p>or click anywhere to browse from your computer</p>
					<span class="file-types"> <i class="fa-solid fa-file-excel"></i>

						XLSX / XLS <span class="dot">•</span> Maximum 25 MB

					</span>


					<button type="button" id="browseBtn" class="browse-btn">

						<i class="fa-solid fa-folder-open"></i> Browse File

					</button>

				</div>



				<!-- SELECTED FILE -->

				<div id="selectedFile" class="selected-file" style="display: none;">


					<div class="selected-file-icon">

						<i class="fa-solid fa-file-excel"></i>

					</div>


					<div class="selected-file-info">

						<strong id="selectedFileName"> file.xlsx </strong> <span
							id="selectedFileSize"> 0 KB </span>

					</div>


					<button type="button" id="removeFile" class="remove-file-btn">

						<i class="fa-solid fa-trash"></i>

					</button>

				</div>

				<!-- <div id="validationResult" class="validation-result"
					style="display: none;"></div> -->

				<div id="validationResult" class="validation-result"
					style="display: none;">

					<div class="validation-result-header">

						<div class="validation-result-icon">
							<i class="fa-solid fa-circle-check"></i>
						</div>

						<div>

							<h4 id="validationResultTitle">File Validation Successful</h4>

							<p id="validationResultMessage">Your file is ready for
								migration.</p>

						</div>

					</div>


					<div class="validation-summary">

						<div class="validation-summary-item">

							<span>Header Row</span> <strong id="validationHeaderRow">
								- </strong>

						</div>


						<div class="validation-summary-item">

							<span>Data Rows</span> <strong id="validationDataRows">
								- </strong>

						</div>


						<div class="validation-summary-item">

							<span>Columns</span> <strong id="validationColumnCount">
								- </strong>

						</div>

					</div>


					<div id="validationErrors" class="validation-errors"
						style="display: none;">

						<strong> <i class="fa-solid fa-triangle-exclamation"></i>
							Validation Errors
						</strong>

						<ul id="validationErrorList"></ul>

					</div>

				</div>

				<!-- ACTIONS -->

				<div class="upload-actions">


					<a href="<c:url value='/migration/template/${moduleCode}'/>"
						id="downloadTemplate" class="template-btn"> <i
						class="fa-solid fa-download"></i> Download Template

					</a>


					<div class="primary-actions">


						<button type="button" id="resetBtn" class="reset-btn">

							<i class="fa-solid fa-rotate-left"></i> Reset

						</button>


						<button type="button" id="validateBtn" class="validate-btn"
							disabled>

							<i class="fa-solid fa-shield-check"></i> Validate File

						</button>

						<button type="button" id="processBtn" class="process-btn" disabled>

							<i class="fa-solid fa-play"></i> Process Migration

						</button>

					</div>

				</div>

			</section>



			<!-- =================================================
                 MIGRATION INFORMATION
            ================================================== -->

			<aside class="upload-side-panel">


				<!-- FILE REQUIREMENTS -->

				<div class="side-card">


					<div class="side-card-header">

						<div class="side-icon blue">

							<i class="fa-solid fa-circle-info"></i>

						</div>


						<div>

							<h4>File Requirements</h4>

							<span> Before uploading </span>

						</div>

					</div>


					<ul class="requirements-list">

						<li><i class="fa-solid fa-circle-check"></i> Excel format
							(.xlsx / .xls)</li>


						<li><i class="fa-solid fa-circle-check"></i> Maximum file
							size 25 MB</li>


						<li><i class="fa-solid fa-circle-check"></i> Use the provided
							template</li>


						<li><i class="fa-solid fa-circle-check"></i> Keep mandatory
							columns populated</li>


					</ul>

				</div>



				<!-- MIGRATION FLOW -->

				<div class="side-card">


					<div class="side-card-header">

						<div class="side-icon purple">

							<i class="fa-solid fa-route"></i>

						</div>


						<div>

							<h4>Migration Flow</h4>

							<span> What happens next? </span>

						</div>

					</div>


					<div class="flow-list">


						<div class="flow-item">

							<span class="flow-number"> 1 </span>

							<div>

								<strong> Upload </strong> <small> Select your Excel file
								</small>

							</div>

						</div>


						<div class="flow-item">

							<span class="flow-number"> 2 </span>

							<div>

								<strong> Validate </strong> <small> Check records and
									fields </small>

							</div>

						</div>


						<div class="flow-item">

							<span class="flow-number"> 3 </span>

							<div>

								<strong> Process </strong> <small> Create migration
									records </small>

							</div>

						</div>


						<div class="flow-item">

							<span class="flow-number"> 4 </span>

							<div>

								<strong> Result </strong> <small> View success and
									failures </small>

							</div>

						</div>


					</div>

				</div>

			</aside>

		</div>


		<section id="migrationResultSection" class="migration-result-section"
			style="display: none;">

			<!-- =====================================================
         RESULT HEADER
    ====================================================== -->

			<div class="migration-result-header">

				<div class="result-title-area">

					<div class="result-title-icon">
						<i class="fa-solid fa-chart-column"></i>
					</div>

					<div>
						<h2>Migration Result</h2>

						<p id="resultSummary">Migration processing completed.</p>
					</div>

				</div>

				<div id="resultStatusBadge" class="result-status-badge">

					<i class="fa-solid fa-circle-check"></i> Completed

				</div>

			</div>


			<!-- =====================================================
         SUMMARY CARDS
    ====================================================== -->

			<div class="result-summary-grid">

				<!-- TOTAL -->

				<div class="result-summary-card total-card">

					<div class="summary-icon">
						<i class="fa-solid fa-layer-group"></i>
					</div>

					<div class="summary-content">

						<span>Total Records</span> <strong id="resultTotal"> 0 </strong>

					</div>

				</div>


				<!-- SUCCESS -->

				<div class="result-summary-card success-card">

					<div class="summary-icon">
						<i class="fa-solid fa-circle-check"></i>
					</div>

					<div class="summary-content">

						<span>Successful</span> <strong id="resultSuccess"> 0 </strong>

					</div>

				</div>


				<!-- FAILED -->

				<div class="result-summary-card failed-card">

					<div class="summary-icon">
						<i class="fa-solid fa-circle-xmark"></i>
					</div>

					<div class="summary-content">

						<span>Failed</span> <strong id="resultFailed"> 0 </strong>

					</div>

				</div>


				<!-- SKIPPED -->

				<div class="result-summary-card skipped-card">

					<div class="summary-icon">
						<i class="fa-solid fa-forward"></i>
					</div>

					<div class="summary-content">

						<span>Skipped</span> <strong id="resultSkipped"> 0 </strong>

					</div>

				</div>

			</div>



			<!-- =====================================================
         FAILED RECORDS
    ====================================================== -->

			<div id="failedRecordsSection"
				class="result-table-section failed-result-section"
				style="display: none;">

				<!-- HEADER -->

				<div class="result-table-header">

					<div class="table-title-area">

						<div class="table-title-icon failed-icon">

							<i class="fa-solid fa-circle-xmark"></i>

						</div>

						<div>

							<h3>Failed Records</h3>

							<p>Records that could not be migrated</p>

						</div>

					</div>


					<span id="failedCountBadge" class="record-count-badge failed-badge">

						0 </span>

				</div>


				<!-- TABLE -->

				<div class="result-table-wrapper">

					<table class="migration-result-table">

						<thead>

							<tr>

								<th style="width: 70px;">#</th>

								<th style="width: 130px;">EXCEL ROWS</th>

								<th style="width: 120px;">STATUS</th>

								<th>ERROR / REASON</th>

								<th style="width: 100px;">TIME</th>

							</tr>

						</thead>

						<tbody id="failedRecordsBody">

						</tbody>

					</table>

				</div>


				<!-- PAGINATION -->

				<div class="result-pagination" id="failedPagination">

					<div class="pagination-info" id="failedPaginationInfo">

						Showing 0-0 of 0</div>


					<div class="pagination-buttons">

						<button type="button" class="pagination-btn" id="failedPrevBtn"
							onclick="changeMigrationPage('failed', -1)">

							<i class="fa-solid fa-chevron-left"></i>

						</button>


						<div id="failedPageNumbers" class="page-numbers"></div>


						<button type="button" class="pagination-btn" id="failedNextBtn"
							onclick="changeMigrationPage('failed', 1)">

							<i class="fa-solid fa-chevron-right"></i>

						</button>

					</div>

				</div>

			</div>



			<!-- =====================================================
         SUCCESSFUL RECORDS
    ====================================================== -->

			<div id="successRecordsSection"
				class="result-table-section success-result-section"
				style="display: none;">

				<!-- HEADER -->

				<div class="result-table-header">

					<div class="table-title-area">

						<div class="table-title-icon success-icon">

							<i class="fa-solid fa-circle-check"></i>

						</div>

						<div>

							<h3>Successful Records</h3>

							<p>Records successfully migrated</p>

						</div>

					</div>


					<span id="successCountBadge"
						class="record-count-badge success-badge"> 0 </span>

				</div>


				<!-- TABLE -->

				<div class="result-table-wrapper">

					<table class="migration-result-table">

						<thead>

							<tr>

								<th style="width: 70px;">#</th>

								<th style="width: 130px;">EXCEL ROWS</th>

								<th style="width: 120px;">STATUS</th>

								<th>MESSAGE</th>

								<th style="width: 100px;">TIME</th>

							</tr>

						</thead>


						<tbody id="successRecordsBody">

						</tbody>

					</table>

				</div>


				<!-- PAGINATION -->

				<div class="result-pagination" id="successPagination">

					<div class="pagination-info" id="successPaginationInfo">

						Showing 0-0 of 0</div>


					<div class="pagination-buttons">

						<button type="button" class="pagination-btn" id="successPrevBtn"
							onclick="changeMigrationPage('success', -1)">

							<i class="fa-solid fa-chevron-left"></i>

						</button>


						<div id="successPageNumbers" class="page-numbers"></div>


						<button type="button" class="pagination-btn" id="successNextBtn"
							onclick="changeMigrationPage('success', 1)">

							<i class="fa-solid fa-chevron-right"></i>

						</button>

					</div>

				</div>

			</div>



			<!-- =====================================================
         SKIPPED RECORDS
    ====================================================== -->

			<div id="skippedRecordsSection"
				class="result-table-section skipped-result-section"
				style="display: none;">

				<!-- HEADER -->

				<div class="result-table-header">

					<div class="table-title-area">

						<div class="table-title-icon skipped-icon">

							<i class="fa-solid fa-forward"></i>

						</div>

						<div>

							<h3>Skipped Records</h3>

							<p>Duplicate records skipped during migration</p>

						</div>

					</div>


					<span id="skippedCountBadge"
						class="record-count-badge skipped-badge"> 0 </span>

				</div>


				<!-- TABLE -->

				<div class="result-table-wrapper">

					<table class="migration-result-table">

						<thead>

							<tr>

								<th style="width: 70px;">#</th>

								<th style="width: 130px;">EXCEL ROWS</th>

								<th style="width: 120px;">STATUS</th>

								<th>REASON</th>

								<th style="width: 100px;">TIME</th>

							</tr>

						</thead>


						<tbody id="skippedRecordsBody">

						</tbody>

					</table>

				</div>


				<!-- PAGINATION -->

				<div class="result-pagination" id="skippedPagination">

					<div class="pagination-info" id="skippedPaginationInfo">

						Showing 0-0 of 0</div>


					<div class="pagination-buttons">

						<button type="button" class="pagination-btn" id="skippedPrevBtn"
							onclick="changeMigrationPage('skipped', -1)">

							<i class="fa-solid fa-chevron-left"></i>

						</button>


						<div id="skippedPageNumbers" class="page-numbers"></div>


						<button type="button" class="pagination-btn" id="skippedNextBtn"
							onclick="changeMigrationPage('skipped', 1)">

							<i class="fa-solid fa-chevron-right"></i>

						</button>

					</div>

				</div>

			</div>

		</section>


		<!-- =================================================
             STATUS CARD
        ================================================== -->

		<section class="upload-status-card">


			<div class="status-left">


				<div class="status-icon idle">

					<i class="fa-solid fa-hourglass-start"></i>

				</div>


				<div>

					<strong id="uploadStatus"> Ready to Upload </strong> <span
						id="uploadStatusText"> Select an Excel file to begin the
						migration process. </span>

				</div>

			</div>


			<div class="status-badge idle" id="statusBadge">

				<span></span> Idle

			</div>

		</section>


	</main>


	<!-- FOOTER -->
	<%@ include file="../layout/foot.jsp"%>
</body>

</html>