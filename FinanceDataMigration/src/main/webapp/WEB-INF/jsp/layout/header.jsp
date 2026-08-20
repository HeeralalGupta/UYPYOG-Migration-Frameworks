<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<header class="app-header">

	<!-- LEFT : BRAND -->
	<div class="header-left">

		<div class="logo-circle">
			<i class="fa-solid fa-building-columns"></i>
		</div>

		<div class="brand-info">

			<h2 class="app-title">
				Finance Data Migration Framework
			</h2>

			<div class="app-subtitle">
				Enterprise Migration Platform
			</div>

		</div>

	</div>


	<!-- RIGHT : HEADER ACTIONS -->
	<div class="header-right">

		<!-- VERSION -->
		<div class="version-box">

			<div class="version-icon">
				<i class="fa-solid fa-code-branch"></i>
			</div>

			<div class="version-content">
				<span class="version-label">
					Version
				</span>

				<span class="version-value">
					1.0
				</span>
			</div>

		</div>


		<!-- DATE / TIME -->
		<div class="datetime-box">

			<div class="datetime-icon">
				<i class="fa-regular fa-calendar"></i>
			</div>

			<div class="datetime-content">

				<span class="datetime-label">
					Current Date
				</span>

				<span id="currentDate" class="datetime-value">
				</span>

			</div>

		</div>


		<!-- THEME -->
		<button id="themeToggle"
				class="theme-btn"
				type="button"
				title="Toggle Theme">

			<i class="fa-solid fa-moon"></i>

		</button>


		<!-- USER -->
		<div class="user-box">

			<div class="user-avatar">

				<i class="fa-solid fa-user"></i>

			</div>

			<div class="user-info">

				<div class="user-name">
					Administrator
				</div>

				<div class="user-role">
					Finance Administrator
				</div>

			</div>

			<i class="fa-solid fa-chevron-down user-arrow"></i>

		</div>

	</div>

</header>