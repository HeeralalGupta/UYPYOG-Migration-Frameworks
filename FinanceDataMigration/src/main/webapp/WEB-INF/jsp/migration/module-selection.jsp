<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>

<head>

<meta charset="UTF-8">

<title>Select Migration | Finance Data Migration Framework</title>
<%@ include file="../layout/head.jsp"%>

</head>


<body>

	<!-- HEADER -->
	<%@ include file="../layout/header.jsp"%>

	<!-- NAVIGATION -->
	<%@ include file="../layout/navbar.jsp"%>

	<main class="migration-page">

		<!-- =========================================
		     PAGE HEADER
		========================================= -->

		<section class="migration-page-header">

			<div class="page-heading">

				<div class="page-heading-icon">

					<i class="fa-solid fa-cloud-arrow-up"></i>

				</div>


				<div>

					<div class="page-eyebrow">

						<i class="fa-solid fa-layer-group"></i> MIGRATION WORKSPACE

					</div>


					<h1>Start New Migration</h1>


					<p>Select a migration category and choose the data you want to
						import.</p>

				</div>

			</div>


			<a href="<c:url value='/migration'/>" class="back-btn"> <i
				class="fa-solid fa-arrow-left"></i> Back to Dashboard

			</a>

		</section>



		<!-- =========================================
		     SEARCH
		========================================= -->

		<section class="module-toolbar">

			<div class="module-count">

				<div class="toolbar-icon">

					<i class="fa-solid fa-cubes"></i>

				</div>

				<div>

					<strong id="moduleCount"> 0 </strong> <span> Migration
						Modules </span>

				</div>

			</div>


			<div class="module-search">

				<i class="fa-solid fa-magnifying-glass"></i> <input type="text"
					id="moduleSearch" placeholder="Search migration modules...">

				<button type="button" id="clearSearch" class="clear-search">

					<i class="fa-solid fa-xmark"></i>

				</button>

			</div>

		</section>



		<!-- =========================================
		     MODULE CATEGORIES
		========================================= -->

		<section class="module-container">

			<div id="moduleAccordion">

				<!-- JavaScript renders categories here -->

			</div>


			<!-- Empty Search State -->

			<div id="moduleEmptyState" class="module-empty-state"
				style="display: none;">

				<div class="empty-module-icon">

					<i class="fa-solid fa-magnifying-glass"></i>

				</div>

				<h3>No Migration Found</h3>

				<p>Try searching with another module name.</p>

				<button type="button" id="resetModuleSearch"
					class="reset-search-btn">

					<i class="fa-solid fa-rotate-left"></i> Clear Search

				</button>

			</div>

		</section>


		<!-- =========================================
		     INFORMATION
		========================================= -->

		<section class="migration-info">

			<div class="info-icon">

				<i class="fa-solid fa-circle-info"></i>

			</div>

			<div>

				<strong> Before you begin </strong>

				<p>Choose the migration type carefully. Each migration has its
					own template, validation rules and processing workflow.</p>

			</div>

		</section>


	</main>


	<!-- FOOTER -->

	<%@ include file="../layout/foot.jsp"%>

</body>

</html>