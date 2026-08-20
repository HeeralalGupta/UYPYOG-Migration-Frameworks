<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Finance Data Migration Framework</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/base.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/layout.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/header.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/navbar.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/footer.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/dashboard.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/theme.css">
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css">
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css"
	rel="stylesheet">

</head>

<body>

	<%@ include file="../layout/header.jsp"%>

	<%@ include file="../layout/navbar.jsp"%>

	<div class="container-fluid mt-4">

		<div class="welcome-section">
		
		    <div class="welcome-content">
		
		        <div class="welcome-icon">
		            <i class="fa-solid fa-wand-magic-sparkles"></i>
		        </div>
		
		        <div class="welcome-text">
		
		            <div class="welcome-eyebrow">
		                <i class="fa-solid fa-circle-check"></i>
		                Migration Workspace
		            </div>
		
		            <h2>
		                Welcome, Administrator
		                <span class="welcome-wave">👋</span>
		            </h2>
		
		            <p>
		                Manage enterprise data migration with a secure,
		                scalable and reusable migration framework.
		            </p>
		
		        </div>
		
		    </div>
		
		
		    <div class="welcome-action">
		
		        <a href="<c:url value='/migration/new'/>"
		           class="welcome-btn">
		
		            <span class="welcome-btn-icon">
		                <i class="fa-solid fa-rocket"></i>
		            </span>
		
		            <span>
		                Start New Migration
		            </span>
		
		            <i class="fa-solid fa-arrow-right welcome-arrow"></i>
		
		        </a>
		
		    </div>
		
		</div>

		<div class="row g-4 mt-4">

			<!-- Modules -->
			<div class="col-xl-3 col-lg-6 col-md-6">

				<div class="dashboard-card blue-card">

					<div class="card-circle">
						<i class="fa-solid fa-layer-group"></i>
					</div>

					<div class="card-details">

						<div class="card-label">Migration Modules</div>

						<div class="card-number">7</div>

						<div class="card-footer">

							<span> <i class="fa-solid fa-circle-check"></i> Available
							</span> <span class="trend"> +2 </span>

						</div>

					</div>

				</div>

			</div>

			<!-- Jobs -->

			<div class="col-xl-3 col-lg-6 col-md-6">

				<div class="dashboard-card green-card">

					<div class="card-circle">
						<i class="fa-solid fa-database"></i>
					</div>

					<div class="card-details">

						<div class="card-label">Total Jobs</div>

						<div class="card-number">15</div>

						<div class="card-footer">

							<span> <i class="fa-solid fa-arrow-trend-up"></i> Today

							</span> <span class="trend"> +5 </span>

						</div>

					</div>

				</div>

			</div>

			<!-- Success -->

			<div class="col-xl-3 col-lg-6 col-md-6">

				<div class="dashboard-card orange-card">

					<div class="card-circle">

						<i class="fa-solid fa-chart-line"></i>

					</div>

					<div class="card-details">

						<div class="card-label">Success Rate</div>

						<div class="card-number">98.6%</div>

						<div class="card-footer">

							<span> Excellent </span>

						</div>

					</div>

				</div>

			</div>

			<!-- Running -->

			<div class="col-xl-3 col-lg-6 col-md-6">

				<div class="dashboard-card purple-card">

					<div class="card-circle">

						<i class="fa-solid fa-play"></i>

					</div>

					<div class="card-details">

						<div class="card-label">Running Jobs</div>

						<div class="card-number">1</div>

						<div class="card-footer">

							<span> Processing </span>

						</div>

					</div>

				</div>

			</div>

		</div>

		<div class="row g-4 mt-4">

			<!-- =========================
         QUICK ACTIONS
    ========================== -->
			<div class="col-xl-5 col-lg-12">

				<div class="dashboard-panel quick-panel">

					<div class="panel-header">

						<div class="panel-title">

							<div class="panel-icon blue-icon">
								<i class="fa-solid fa-bolt"></i>
							</div>

							<div>
								<h4>Quick Actions</h4>
								<p>Frequently used migration tools</p>
							</div>

						</div>

					</div>


					<div class="quick-actions-grid">

						<!-- New Migration -->
						<a href="<c:url value='/migration/new'/>"
							class="quick-action-card primary-action">

							<div class="action-icon">
								<i class="fa-solid fa-cloud-arrow-up"></i>
							</div>

							<div class="action-content">

								<strong>Start Migration</strong> <span> Import new data </span>

							</div> <i class="fa-solid fa-arrow-right action-arrow"></i>

						</a>


						<!-- History -->
						<a href="<c:url value='/migration/history'/>"
							class="quick-action-card green-action">

							<div class="action-icon">
								<i class="fa-solid fa-clock-rotate-left"></i>
							</div>

							<div class="action-content">

								<strong>Migration History</strong> <span> View previous
									jobs </span>

							</div> <i class="fa-solid fa-arrow-right action-arrow"></i>

						</a>


						<!-- Reports -->
						<a href="<c:url value='/migration/reports'/>"
							class="quick-action-card purple-action">

							<div class="action-icon">
								<i class="fa-solid fa-chart-column"></i>
							</div>

							<div class="action-content">

								<strong>Reports & Analytics</strong> <span> Analyze
									migration data </span>

							</div> <i class="fa-solid fa-arrow-right action-arrow"></i>

						</a>


						<!-- Administration -->
						<a href="<c:url value='/migration/settings'/>"
							class="quick-action-card orange-action">

							<div class="action-icon">
								<i class="fa-solid fa-gear"></i>
							</div>

							<div class="action-content">

								<strong>Administration</strong> <span> Configure
									framework </span>

							</div> <i class="fa-solid fa-arrow-right action-arrow"></i>

						</a>

					</div>

				</div>

			</div>


			<!-- =========================
         RECENT MIGRATION JOBS
    ========================== -->
			<div class="col-xl-7 col-lg-12">

				<div class="dashboard-panel recent-panel">

					<div class="panel-header">

						<div class="panel-title">

							<div class="panel-icon purple-icon">
								<i class="fa-solid fa-clock-rotate-left"></i>
							</div>

							<div>

								<h4>Recent Migration Jobs</h4>

								<p>Latest migration activity</p>

							</div>

						</div>


						<a href="<c:url value='/migration/history'/>" class="view-all-btn">

							View All <i class="fa-solid fa-arrow-right"></i>

						</a>

					</div>


					<div class="table-wrapper">

						<table class="migration-table">

							<thead>

								<tr>

									<th>Job ID</th>

									<th>Module</th>

									<th>Tenant</th>

									<th>Status</th>

									<th>Started</th>

									<th>Duration</th>

									<th></th>

								</tr>

							</thead>


							<tbody>

								<!-- Empty State -->

								<tr>

									<td colspan="7">

										<div class="empty-state">

											<div class="empty-icon">

												<i class="fa-solid fa-inbox"></i>

											</div>

											<strong> No Migration Jobs </strong> <span> No
												migration has been executed yet. </span> <a
												href="<c:url value='/migration/new'/>" class="empty-action">

												<i class="fa-solid fa-plus"></i> Start Your First Migration

											</a>

										</div>

									</td>

								</tr>

							</tbody>

						</table>

					</div>

				</div>

			</div>

		</div>

	</div>

	<%@ include file="../layout/footer.jsp"%>
	<script src="${pageContext.request.contextPath}/js/app.js"></script>
	<script src="${pageContext.request.contextPath}/js/theme.js"></script>
	<script src="${pageContext.request.contextPath}/js/navigation.js"></script>
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>