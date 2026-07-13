You are a senior Angular engineer. Build a complete, production-quality Angular frontend prototype from scratch in the current empty (or new) repository. Do not invent a live backend, ServiceNow, or real CIAM APIs. Implement the full application described below as specified — same domain model, routes, workflow gates, mock data, and UX. Work in phases; keep the app compiling after each phase. Prefer complete working code over stubs.

Product
Name: Spider CIAM Self-Service Onboarding Portal (spider-ciam-portal) Goal: Guided self-service portal for consumer app teams onboarding applications to US CIAM — structured intake → standard integration patterns (Auth / Registration / MFA) → Lab package → GRC → DIT/SIT/PAT testing → production readiness. Prototype only (0→1 demo). Success demo: A designated Spider CIAM Test Client request can be shown end-to-end at READY_FOR_PRODUCTION with standard pattern AUTHENTICATION_REGISTRATION_MFA and no bespoke design.

Tech stack (mandatory)
Angular 22 standalone components (no NgModules)
TypeScript strict + strictTemplates
SCSS for component styles; global design tokens via CSS variables
Signals / computed / effect / toSignal for UI state
RxJS only where Angular APIs require it (routing)
Vitest via ng test (jsdom)
No NgRx, no Angular Material — use custom accessible UI
No any in app code
Business rules live in services; components orchestrate UI only
Repository pattern: OnboardingRequestRepository token + LocalStorageOnboardingRequestRepository
provideHttpClient() wired for future APIs but unused
Bootstrap: provideAppInitializer → seed mock data when store empty
Node.js 22+ required
Scaffold with Angular CLI if needed:

npx @angular/cli@22 new spider-ciam-portal --routing --style=scss --ssr=false --skip-git

Then implement everything in src/app/.

Folder structure
src/app/ core/ models/ # enums + request/questionnaire/audit/environment models data/ # seeds, questionnaire, guides, checklists, resources, integration-kit-mock repositories/ # OnboardingRequestRepository + localStorage impl services/ # all business logic guards/ # roleGuard utils/ # display label helpers layout/ # app-shell, header (role switcher), nav shared/components/ # page-header, status-chip, empty-state, confirm-dialog (focus trap + Escape), # dynamic-question form/field, request-workspace-nav, request-comments-panel features/ landing/ guide/ requests/ # list, intake wizard, detail, package, environments, testing, readiness ciam/ # dashboard + review grc/ # dashboard + review admin/ # questionnaire CRUD + resources placeholder not-found/

Persistence keys
spider-ciam-requests-v1 (localStorage) — Onboarding requests
spider-ciam-questionnaire-v1 (localStorage) — Admin questionnaire config
spider-ciam-demo-role (sessionStorage) — Active demo role
spider-ciam-wizard-v1:{requestId|new} (sessionStorage) — Intake wizard draft
Demo roles (UserRole)
CONSUMER / Consumer App Team / demo.consumer@example.com
CIAM_REVIEWER / CIAM Reviewer / ciam.reviewer@example.com
GRC_APPROVER / GRC Approver / grc.approver@example.com
ADMIN / Administrator / admin@example.com
Header role switcher updates AuthContextService + sessionStorage. Not real auth. roleGuard redirects unauthorized users to /.

Enums (exact names)
Implement in core/models/enums.ts:

RequestStatus: DRAFT, SUBMITTED, CIAM_REVIEW, MORE_INFORMATION_REQUIRED, GRC_REVIEW, LAB_ENABLEMENT_AVAILABLE, GRC_APPROVED, GRC_REJECTED, DIT_READY, DIT_IN_PROGRESS, DIT_ACCEPTED, SIT_READY, SIT_IN_PROGRESS, SIT_ACCEPTED, PAT_READY, PAT_IN_PROGRESS, PAT_ACCEPTED, READY_FOR_PRODUCTION, CLOSED
Capability: AUTHENTICATION, REGISTRATION, MFA, API_INTEGRATION
Environment: LAB, DIT, SIT, PAT, PROD
RequestType: NEW_ONBOARDING, CAPABILITY_EXPANSION, ENVIRONMENT_EXPANSION, CHANGE_REQUEST
ApplicationType: WEB, SPA, MOBILE, API, BATCH, OTHER
DataClassification: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
GrcDecisionStatus: NOT_REQUIRED, PENDING, APPROVED, REJECTED, MORE_INFORMATION_REQUIRED
AccessStatus: LOCKED, AVAILABLE, ENABLED, COMPLETE
TestingStatus: NOT_STARTED, IN_PROGRESS, BLOCKED, ACCEPTED
SecurityAssessmentStatus: NOT_STARTED, IN_PROGRESS, COMPLETE, NOT_APPLICABLE
IntegrationPattern: AUTHENTICATION, REGISTRATION, MFA, AUTHENTICATION_REGISTRATION, AUTHENTICATION_MFA, AUTHENTICATION_REGISTRATION_MFA, API_INTEGRATION, EXCEPTION_CUSTOM
MfaTiming: EVERY_LOGIN, STEP_UP, RISK_BASED, UNKNOWN
ApiAuthMethod: CLIENT_CREDENTIALS, AUTHORIZATION_CODE, MTLS, API_KEY, OTHER
QuestionFieldType: text, textarea, select, multiselect, checkbox, radio, date, url, environment_url_table, repeatable_rows
Domain model (OnboardingRequest)
Root fields: id, requestNumber, status, createdBy, createdAt, updatedAt, requestType, applicationDetails, selectedCapabilities, authenticationDetails, registrationDetails, mfaDetails, apiDetails, securityDetails, requestedEnvironments, ciamAssessment, grcReview, selfServicePackage, environmentProgress (map per Environment), productionReadiness, comments[], auditHistory[].

ApplicationDetails: applicationName, applicationId, applicationDescription, businessOwner, technicalContact, supportContact, targetGoLiveDate, applicationType, technologyStack, customerFacing, userPopulation.

AuthenticationDetails: supportsOidcOAuth, applicationType, hasBackendOrBff, redirectUrisByEnvironment, logoutUrisByEnvironment, requiredScopes, requiredClaims, singleLogoutRequired.

RegistrationDetails: selfServiceRegistration, attributesToCollect, emailVerificationRequired, phoneVerificationRequired, profileCreationRequired, umpIntegrationRequired, postRegistrationRedirect.

MfaDetails: mfaTiming, preferredMethods, stepUpRequired, stepUpActions, riskBasedRequired, assuranceLevelRequired.

ApiDetails: consumerCallsCiamApi, ciamCallsConsumerApi, endpointsByEnvironment, authenticationMethod, requiredScopes.

SecurityDetails: accessesCustomerData, processesSensitiveData, dataClassification, requestedClaims, claimsJustification, securityAssessmentStatus, privacyAssessmentRequired, grcApprover.

CiamAssessment: assignedPattern, reviewer, reviewComments, missingInformation[], packagePublished.

GrcReview: status, approver, decisionDate, comments.

SelfServicePackage: published, publishedAt, resourceIds[], labOnlyNoticeAcknowledged.

EnvironmentProgressItem: environment, accessStatus, testingStatus, checklistItems[], evidence[], acceptedBy, acceptedAt, defectsOrRisks, comments.

ProductionReadiness: grcApproved, ditAccepted, sitAccepted, patAccepted, productionRedirectUriConfirmed, productionLogoutUriConfirmed, productionConfigConfirmed, secretHandlingConfirmed, supportContactConfirmed, goLiveDateConfirmed, rollbackPlanConfirmed, markedReadyBy, markedReadyAt.

CommentEntry: id, timestamp, author, role, body. AuditHistoryEntry: timestamp, actor, action, previousStatus, newStatus, comments.

Provide factory helpers: createEmptyOnboardingRequest, empty detail creators, empty env progress, empty production readiness.

Routes + role access
/ — public — Landing
/guide, /guide/:slug — public — Guide hub/detail
/requests — CONSUMER, ADMIN
/requests/new — CONSUMER, ADMIN
/requests/:id/edit — CONSUMER, ADMIN, CIAM_REVIEWER
/requests/:id — CONSUMER, ADMIN, CIAM_REVIEWER, GRC_APPROVER
/requests/:id/package — WORKSPACE = Consumer+Admin+CIAM+GRC
/requests/:id/environments — WORKSPACE
/requests/:id/testing — WORKSPACE
/requests/:id/readiness — WORKSPACE
/ciam/dashboard, /ciam/requests/:id/review — CIAM_REVIEWER, ADMIN
/grc/dashboard, /grc/requests/:id/review — GRC_APPROVER, ADMIN
/admin/questionnaire — ADMIN
/admin/resources — ADMIN (placeholder)
** — Not found
Critical routing rule: Register child routes :id/edit, :id/package, :id/environments, :id/testing, :id/readiness BEFORE :id so they are not swallowed by the detail route.

Lazy-load feature route files. Shared RequestWorkspaceNavComponent tabs on request pages: Overview · Edit · Package · Environments · Testing · Readiness.

Nav by role: Consumer sees Requests; CIAM sees CIAM dashboard; GRC sees GRC dashboard; Admin sees all + Admin.

Core services to implement
AuthContextService — current role/email; role switcher
OnboardingRequestService — CRUD over repository; list/get/save; generate request numbers CIAM-2026-NNNN
MockDataService — if requests empty, seed 7 demo requests
WorkflowService — all gate helpers: canSubmit, requiresGrcReview, canPublishLabPackage, canAcceptForCiamAssessment, canRequestMoreInformation, canAssignIntegrationPattern, canSendToGrc, canEnable/Accept Dit/Sit/Pat, canMarkReadyForProduction, canMakeGrcDecision, recommended actions
IntakeWizardService — 5 steps, session draft, validation, map questionnaire answers → typed detail blocks, submit → SUBMITTED + audit
QuestionnaireService — load seed or localStorage; admin CRUD; reset defaults
DynamicFormService — visibility/validation for question definitions
CiamReviewService — accept, more info, assign pattern, select resources, publish Lab package, send to GRC
GrcService — approve / reject / more info; on approve unlock DIT when requested
ResourcePackageService — package view from resource catalog + Lab-only banner until GRC approved
EnvironmentProgressService — unlock rules Lab→Prod
TestingService — DIT/SIT/PAT checklists, evidence mock, accept → unlock next env/status
ProductionReadinessService — sync derived flags, confirmable keys, mark ready
IntegrationKitService — patterns, configs, code samples, DIT self-serve kit personalized per request, standard principles, controlled rollout mock
GuideContentService — articles by slug
RequestFilterService — list filters
RequestCommentService — all roles can comment; audit COMMENT_ADDED
AuditHistoryService — append entries
Do not write side-effecting signal updates inside computed (causes NG0600). Refresh lists in constructors/effects/actions instead.

Workflow rules (exact)
Submit minimum: applicationName, businessOwner, technicalContact, targetGoLiveDate; at least 1 capability; at least 1 environment; review acknowledgements (info accurate; higher envs may need GRC; Lab test client is Lab-only).

GRC required when: DIT/SIT/PAT/PROD requested OR accessesCustomerData OR processesSensitiveData OR classification CONFIDENTIAL/RESTRICTED.

Happy path: DRAFT → SUBMITTED → CIAM_REVIEW and/or LAB_ENABLEMENT_AVAILABLE → GRC_REVIEW → GRC_APPROVED → DIT_READY → DIT_IN_PROGRESS → DIT_ACCEPTED → SIT_READY → … → PAT_ACCEPTED → READY_FOR_PRODUCTION. Side: MORE_INFORMATION_REQUIRED, GRC_REJECTED, CLOSED.

Environment unlocks:

LAB when Lab package published
DIT when GRC APPROVED and DIT requested
SIT when SIT requested and DIT accepted
PAT when PAT requested and SIT accepted
PROD when PROD requested and PAT accepted and READY_FOR_PRODUCTION
Production readiness: 4 system gates (grc/dit/sit/pat) + 7 confirmations (redirect, logout, config, secrets, support, go-live, rollback). All 11 must be true to mark ready. Editable at PAT_ACCEPTED / READY_FOR_PRODUCTION.

Lab test client: Always document as Lab learning / reference only. Formal DIT/SIT/PAT/Prod validation uses the consumer application.

Intake wizard (5 steps)
basics — ApplicationDetails + requestType + requestedEnvironments
capabilities — toggle Capability[]
questionnaire — dynamic questions filtered by selected capabilities (~26 seed questions covering auth/reg/mfa/api; field types including environment_url_table)
security — SecurityDetails
review — summary + acknowledgements + Save draft / Submit
Support create (/requests/new) and edit (/requests/:id/edit). When intake is locked after submit, Consumer may be read-only; CIAM can still open edit for collaboration comments/context as designed.

Pages / UX requirements
Landing: brand-first hero (product name “Spider CIAM” dominant), one headline, one short support line, CTAs (new request / guide), recent requests; not a dashboard clutter
Request list: filters + status chips + empty state
Request detail: status, summary, recommended action, links, comments panel, workspace nav
Package: Lab-only banner; standard pattern only (no bespoke) principles; Integration patterns table with assigned highlight; Configurations (#4) with selectable JSON/YAML/env docs; Code samples (#3); DIT endpoints prep; published resources
Environments: table of Lab→Prod access/testing status
Testing: DIT/SIT/PAT tabs; when DIT selected show full DIT self-serve runbook (issuer, clientId personalized from app id/name, endpoints, steps 1–7, sample claims, evidence filenames) + checklist accept
Readiness: controlled rollout validation table + rollback plan mock; checklist; Mark Ready for Production with confirm dialog
CIAM dashboard/review: queue; accept; more info; assign IntegrationPattern; pick resource ids; publish Lab package; send to GRC. Header: Back to dashboard only (no “Consumer view” link)
GRC dashboard/review: approve/reject/more info. Same header rule
Guide hub/detail: slugs authentication, registration, mfa, test-client, troubleshooting with rich demoScenario, callouts, samples, tables, code
Admin questionnaire: live CRUD persisted to localStorage; reset to defaults
Admin resources: placeholder empty state
Shared: PageHeader, StatusChip, EmptyState, ConfirmDialog (focus trap, Escape, restore focus)
Integration kit mock data (integration-kit-mock.ts)
Must include:

Pattern guides for AUTHENTICATION, AUTHENTICATION_REGISTRATION, AUTHENTICATION_MFA, AUTHENTICATION_REGISTRATION_MFA, API_INTEGRATION (when-to-use + steps)
Code samples: spa-pkce, bff-token-exchange, registration-submit, mfa-step-up
Configs: oidc-client-dit, registration-config-dit, mfa-policy-dit, app-env-file
MOCK_DIT_SELF_SERVE_KIT with authorize/token/userinfo/logout/registration endpoints and 7 steps
STANDARD_PATTERN_PRINCIPLES (no bespoke)
MOCK_CONTROLLED_ROLLOUT (validation steps + rollback plan with owners Alina/Fei/Shihao/Sagar/Tony/Roy, RTO 2h)
IntegrationKitService.getDitKitForRequest personalizes clientId and redirect host from applicationId/name slug.

Self-service resource catalog
Resource ids used in packages: auth-guide, reg-guide, mfa-guide, api-guide, test-client, code-samples, sample-config, lab-checklist, faq — link to guide routes or inline kit content.

Seed requests (7) — create when store empty
CIAM-2026-0001 / Retail Loyalty Portal / DRAFT / Auth draft
CIAM-2026-0002 / Mobile Banking Companion / SUBMITTED / Auth+MFA; seed comments
CIAM-2026-0003 / Customer Self-Registration Hub / GRC_REVIEW / AUTHENTICATION_REGISTRATION; Lab package published
CIAM-2026-0004 / Partner API Gateway / DIT_READY / API_INTEGRATION; GRC approved; DIT unlocked
CIAM-2026-0005 / Wealth Insights Dashboard / SIT_READY / AUTHENTICATION_REGISTRATION_MFA; DIT accepted
CIAM-2026-0006 / Claims Self-Service Portal / PAT_ACCEPTED / AUTHENTICATION_MFA; readiness incomplete
CIAM-2026-0007 / Spider CIAM Test Client / READY_FOR_PRODUCTION / APP-SPIDER-TEST-CLIENT; Auth+Reg+MFA; all readiness true; stakeholders Alina/Fei/Shihao/Sagar/Tony/Roy; full package resources; Prod AVAILABLE
Include realistic auditHistory and some comments on 0002/0003/0007.

UI / design constraints
Accessible, clean enterprise prototype UI (not generic purple AI aesthetic)
CSS variables for color/spacing/type
Expressive but professional typography (not Inter/Roboto defaults if you can load a Google font pair)
Status chips for RequestStatus
Mobile-responsive enough for demo
Empty states with actions
Avoid writing side effects in templates
Tests
Add unit tests for WorkflowService, IntakeWizardService, CiamReviewService, GrcService, TestingService, ProductionReadinessService, EnvironmentProgressService, QuestionnaireService, IntegrationKitService, roleGuard, ConfirmDialog, key services — target 50+ passing tests with ng test --watch=false.

README
Write a full README covering Node 22, start/build/test, architecture, routes, workflow, seeds, storage keys, demo path (Consumer 0006 readiness; CIAM 0002; GRC 0003; DIT 0004; Package kit on 0005/0007; Test client 0007).

Implementation order (follow strictly)
Scaffold Angular 22 + global styles + app shell + role switcher + roleGuard
Models, enums, repository, OnboardingRequestService, MockDataService seeds
WorkflowService + audit
Landing + request list + detail skeleton
Intake wizard + questionnaire + dynamic form components
CIAM dashboard/review
GRC dashboard/review
Package + resource catalog + IntegrationKitService + UI
Environments + Testing (with DIT runbook)
Production readiness + controlled rollout panel
Guide articles
Admin questionnaire
Comments panel + workspace nav everywhere needed
Polish a11y empty states; README; run build + all tests green
Hard constraints
Do not implement ServiceNow
Do not call real CIAM endpoints — mock URLs like https://ciam-dit.example/... only
Do not skip IntegrationPattern assignment or package/testing/readiness
Do not lock Package/Environments/Testing/Readiness to Consumer-only (CIAM/GRC must open them via WORKSPACE roles)
Prefer one coherent design system; keep code typed and services authoritative for gates
When done, npm start must show the portal with 7 seeds, role switcher working, and the full Intake → Ready for Production demo path usable without a backend.

Begin now. Create the project and implement phase by phase until the application matches this specification.
