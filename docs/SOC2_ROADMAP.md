# PII Vault - SOC 2 Type II Audit & Certification Roadmap

**Document Date**: July 2026  
**Audit Timeline**: 24 weeks (6 months)  
**Target Completion**: Q2 2027 (extending slightly beyond Phase 3)  
**Audience**: Leadership, Security Team, Operations, External Auditors

---

## Executive Summary

SOC 2 Type II certification is a critical trust marker for enterprise SaaS products handling sensitive data. Unlike SOC 2 Type I (point-in-time assessment), Type II requires demonstrating effective security controls over a **minimum 6-month observation period**, making it essential for customer confidence and competitive positioning.

**Why SOC 2 Type II?**
- Enterprise customers require it (RFP/procurement requirement)
- Demonstrates sustained control effectiveness (not just good intentions)
- Complements PCI DSS, GDPR, HIPAA compliance
- Includes audit of change management, access control, data retention
- Third-party validation of security claims
- Opens doors to additional regulated markets

**This Roadmap**: Outlines the complete path from preparation through certification, including all five SOC 2 Trust Service Criteria (CC, A, C, CI, and optionally L).

---

## Table of Contents

1. [SOC 2 Fundamentals](#fundamentals)
2. [Pre-Audit Preparation (Weeks 1-4)](#pre-audit)
3. [Control Implementation (Weeks 5-12)](#implementation)
4. [Audit Observation Period (Weeks 13-24)](#observation)
5. [Auditor Engagement & Reporting](#auditor)
6. [Post-Certification Maintenance](#maintenance)

---

## <a name="fundamentals"></a>SOC 2 Fundamentals

### What is SOC 2 Type II?

SOC 2 (Service Organization Control) is an audit framework developed by the American Institute of CPAs (AICPA) to assess how service organizations protect customer data and maintain effective security controls.

**Type II vs. Type I**:
| Aspect | Type I | Type II |
|--------|--------|---------|
| **Assessment Period** | Point-in-time snapshot | 6-12 months of operations |
| **Control Testing** | Design review only | Design + operating effectiveness |
| **Cost** | $20K-$40K | $40K-$100K |
| **Timeline** | 4-6 weeks | 6 months + audit time |
| **Customer Value** | Low | High (enterprise requirement) |
| **Renewal Frequency** | Annual | Annual (requires new observation) |
| **Best For** | Early-stage/POC | Production SaaS platforms |

### The Five SOC 2 Trust Service Criteria (TSCA)

**CC - Security (Common Criteria)**
- Access controls
- Logical security
- Physical security
- System monitoring
- Data classification

**A - Availability**
- System availability
- Performance SLAs
- Disaster recovery procedures

**C - Confidentiality**
- Encryption
- Data protection
- Privacy controls

**CI - Integrity**
- Data accuracy
- Error detection/correction
- Change management
- Segregation of duties

**L - Privacy** (optional, but recommended for PII)
- Privacy policy compliance
- Data subject rights
- Consent management
- Third-party vendor management

### PII Vault's Scope for SOC 2

**In Scope**:
- PII encryption/decryption service
- REST API endpoints
- AWS KMS integration
- PostgreSQL database
- Audit logging system
- Authentication & authorization
- Disaster recovery capabilities
- Change management processes

**Out of Scope** (managed by customer):
- Customer's use of tokens
- Upstream service security
- Customer's retention policies
- Customer's access control to their systems

---

## <a name="pre-audit"></a>Pre-Audit Preparation (Weeks 1-4)

### Week 1: Auditor Selection & Engagement

#### Choose a SOC 2 Auditor

**Big 4 Firms** (Deloitte, EY, KPMG, PwC)
- Pros: Brand recognition, comprehensive, global reach
- Cons: Expensive ($80K-$150K), slow (6+ months)
- **Best for**: Large enterprises, global customers

**Big Audit Firms** (BDO, Grant Thornton, CliftonLarson)
- Pros: Reasonable cost ($50K-$80K), good quality
- Cons: Slower process, may lack SaaS expertise

**Boutique SaaS Audit Firms** (Prescient, Vanta, Drata partners)
- Pros: SaaS-focused, faster ($40K-$60K), experienced
- Cons: Smaller firms, may lack enterprise experience
- **Best for**: SaaS companies, efficient audits

**Recommended Approach for PII Vault**:
1. Use SaaS-focused boutique firm (faster, more affordable)
2. Engagement letter by end of Week 1
3. Audit period starts in Week 5 (allows 4 weeks prep)
4. Observation period: Weeks 5-24 (20 weeks, sufficient for Type II)

#### Engagement Letter Contents

```
Service Organization: Acme Corp (PII Vault)
Service Auditor: [Firm Name]
Audit Period: Week 5, 2026 - Week 24, 2026 (20 weeks)
Scope: SOC 2 Type II (CC, A, C, CI, L)
Cost: $[XX,XXX]
Deliverable: SOC 2 Type II report (AAB, SSAE 18 format)
Exclusions: Customer security responsibilities
Access Requirements: 
  - Quarterly on-site assessments
  - Monthly remote documentation review
  - Real-time access to audit logs, change logs, incident records
```

### Week 2: Gap Assessment

#### Current State vs. SOC 2 Requirements

Conduct internal audit to identify gaps:

**Security Controls Gap Analysis**:
```
Control Area                  Current State          Gap              Priority
───────────────────────────────────────────────────────────────────────────
Access Control                JWT auth, role-based   Needs MFA         HIGH
                              Ready for audit        Multi-factor auth
                                                     for admins

Change Management             Git repos, branches    Needs formal       HIGH
                              No formal process      process
                                                     Approval workflow
                                                     Testing procedures

Incident Response             Email-based            Needs playbook     HIGH
                              No formal procedures   Incident tracking
                                                     Root cause analysis

Data Retention                Soft-delete flagged    Policy unclear     MEDIUM
                              Schedule unclear       Retention SLA
                                                     Hard-delete schedule

Disaster Recovery             None documented        Needs documented   HIGH
                              Backup procedures      RTO/RPO targets
                              unclear                Test procedures

Segregation of Duties         Limited admin users    Needs approval      MEDIUM
                              No formal process      workflow for
                                                     sensitive ops

Physical Security             AWS managed            AWS compliance      LOW
                              Assume compliant       document needed

Monitoring & Logging          Audit logs working     Needs alerting      MEDIUM
                              Limited monitoring     procedures
                                                     Alert escalation

Encryption in Transit         TLS 1.3 configured     Verify enforcement  LOW
                              Assumed working        Test certificate
                                                     validation

Encryption at Rest            KMS encryption         Key rotation        MEDIUM
                              Phase 2 ready          schedule needed
```

**Deliverable**: Gap assessment report (prioritized remediation list)

### Week 3: Documentation Preparation

#### Policies & Procedures (Template List)

Create or update these documents (required for SOC 2):

**Governance**:
- [ ] Security Policy (overall security commitment)
- [ ] Access Control Policy (authentication, authorization, MFA)
- [ ] Change Management Policy (code review, approval, testing)
- [ ] Incident Response Policy (detection, response, resolution, notification)
- [ ] Data Retention & Destruction Policy (retention periods, deletion procedures)
- [ ] Disaster Recovery & Business Continuity Policy (RTO/RPO, failover procedures)
- [ ] Third-Party Risk Management Policy (vendor assessment, monitoring)
- [ ] Compliance Policy (regulatory requirements, audit procedures)

**Technical**:
- [ ] System Architecture Documentation (components, data flow, dependencies)
- [ ] Encryption Policy (algorithm, key management, rotation)
- [ ] Database Security Procedures (backups, replication, access control)
- [ ] Monitoring & Alerting Procedures (metrics, alerts, escalation)
- [ ] Physical Security Procedures (AWS data center controls, key management)
- [ ] Code Review & Testing Procedures (review criteria, security testing)

**Operational**:
- [ ] Onboarding Procedures (new engineer security checklist)
- [ ] Offboarding Procedures (access removal, knowledge transfer)
- [ ] Password Policy (complexity, rotation, management)
- [ ] Security Training Requirements (annual training, incident response drills)
- [ ] Vendor Management Procedures (vendor assessment, contracts, monitoring)

**Status Tracking**:
- [ ] Evidence Tracking Matrix (maps each policy to control objective, evidence location)

#### Evidence Collection Start

Begin organizing evidence repositories:

```
Evidence Structure:
├── Policy_Documents/
│   ├── Security_Policy_v1.0_2026-07-01.pdf
│   ├── AccessControl_Policy_v2.1_2026-07-01.pdf
│   └── ... (all policies dated and versioned)
│
├── Change_Management/
│   ├── Approved_Changes_Log.xlsx
│   ├── Code_Review_Evidence/
│   │   ├── PR_123_security_review.pdf
│   │   └── ... (sample PRs with security review comments)
│   └── Testing_Evidence/
│       ├── Unit_Test_Results_2026-07.pdf
│       └── ... (test execution records)
│
├── Access_Control/
│   ├── User_Provisioning_Log.xlsx
│   ├── Access_Review_Minutes_2026-06.pdf
│   ├── MFA_Implementation_Status.xlsx
│   └── ... (user access records)
│
├── Incident_Response/
│   ├── Incident_Log.xlsx
│   ├── Sample_Incident_Report_20260615.pdf
│   └── ... (incident investigations, resolutions)
│
├── Monitoring_Logs/
│   ├── Audit_Log_Sample_2026-06.csv
│   ├── KMS_Usage_Sample_2026-06.csv
│   ├── Database_Access_Sample_2026-06.csv
│   └── ... (system logs, retention demonstrated)
│
├── Disaster_Recovery/
│   ├── DR_Test_Results_2026-04.pdf
│   ├── Backup_Verification_Log_2026-06.csv
│   ├── RTO_RPO_Targets_v1.0.pdf
│   └── ... (recovery procedures, test results)
│
└── Third_Party_Management/
    ├── Vendor_Risk_Assessment_AWS.pdf
    ├── AWS_Compliance_Documentation.pdf
    ├── Vendor_Monitoring_Log.xlsx
    └── ... (vendor contracts, SLAs, assessments)
```

#### Sample Policy Template (Access Control)

```markdown
# Access Control Policy

## Purpose
Ensure that access to PII Vault systems is restricted to authorized personnel 
and that strong authentication and authorization mechanisms are maintained.

## Scope
All users accessing PII Vault systems, including employees, contractors, and 
third-party service providers.

## Policy

### Authentication
1. All users must authenticate using multi-factor authentication (MFA)
   - Primary: Username + password (PBKDF2 hashed, minimum 12 characters)
   - Secondary: Time-based one-time password (TOTP) via authenticator app
   - Exception: Service-to-service authentication via API keys with rotation

2. Password Requirements
   - Minimum 12 characters
   - At least one uppercase, lowercase, number, special character
   - No dictionary words or personally identifiable information
   - Cannot reuse last 5 passwords
   - Expiration: 90 days for production access, 180 days for development

3. MFA Requirements
   - All production access requires MFA
   - All admin operations require MFA verification
   - Remote access requires MFA
   - Exception process: Documented in incident log, approved by CISO

### Authorization
1. Role-Based Access Control (RBAC)
   - USER: Can encrypt/decrypt PII
   - ADMIN: Can manage tokens, view audit logs, manage users
   - SUPER_ADMIN: Can rotate keys, modify policies, access KMS

2. Least Privilege Principle
   - Users assigned minimum permissions needed for role
   - Access reviews conducted quarterly
   - Unused permissions revoked within 30 days

3. Separation of Duties
   - Key rotation approved by 2 independent admins
   - Production deployments require code review + approval
   - Sensitive data access logged and monitored

### Access Management
1. Provisioning
   - New users provisioned within 2 business days
   - Access request must include business justification and manager approval
   - Provisioning recorded in user provisioning log

2. De-provisioning
   - Terminated employees: All access revoked within 4 hours
   - Changed roles: Access updated within 1 business day
   - De-provisioning recorded in access removal log
   - Access review performed after any major change

3. Access Reviews
   - Managers review direct reports' access quarterly
   - Admin access reviewed monthly by CISO
   - Discrepancies remediated within 2 business days

## Accountability
- Violations reported to CISO
- First violation: Written warning
- Repeated violations: Access suspension, termination

## Effective Date
2026-07-01

## Approval
- CISO: [Name]
- CEO: [Name]

## Revision History
- 2026-07-01: Initial version
```

### Week 4: Control Design & Implementation Planning

#### Map Controls to SOC 2 Criteria

Example mapping (excerpt):

```
SOC 2 Criterion                   Control                         Evidence
──────────────────────────────────────────────────────────────────────────
CC6.1 Logical and physical         MFA for all users               User audit log
access restricted to              RBAC for authorization          Access review minutes
authorized users                  SSH key-based API auth          Access provisioning log
                                  Network segmentation            AWS security group config
                                  (managed by AWS)

CC6.2 Prior to issuing credentials,  Access request form          Approval workflows
identity and authorization      Manager approval required        Email approvals
verified                          Background check (contractor)   Contractor agreements

CC7.1 System monitoring identifies  CloudWatch monitoring         CloudWatch dashboard
potential security incidents      Audit log analysis              Alert configuration
                                  KMS usage tracking              KMS CloudTrail logs
                                  Failed login attempts           Login attempt logs

CC7.2 Monitoring tools configured   Alert thresholds set           Alert configuration
appropriately and alerts           PagerDuty escalation           Incident response log
monitored and acted upon          Response time SLAs              On-call schedule

C1.1 PII encrypted at rest         AES-256-GCM encryption         Encryption config review
                                  KMS master keys                 KMS key audit
                                  Data classification             Data flow diagram

C1.2 PII encrypted in transit      TLS 1.3 minimum                TLS certificate audit
                                  Certificate validation          Certificate management log
                                  HTTPS enforcement               Load balancer config
```

**Deliverable**: Control mapping document (cross-references SOC 2 criteria to controls)

---

## <a name="implementation"></a>Control Implementation (Weeks 5-12)

**Note**: Weeks 5-12 overlap with Phase 3 development work. SOC 2 controls must be implemented in production code and operations.

### Week 5-6: Access Control Hardening

#### Implement Multi-Factor Authentication (MFA)

For admin users accessing PII Vault:

**Phase 1: Enable MFA for Admin Users**
```java
// In AuthenticationController.java
@PostMapping("/admin/login")
public ResponseEntity<AuthResponse> adminLogin(
  @RequestBody AdminLoginRequest request
) {
  // 1. Validate username + password
  UserDetails user = authenticate(request.getUsername(), request.getPassword());
  
  if (!user.getAuthorities().contains("ROLE_ADMIN")) {
    throw new UnauthorizedException("Admin access required");
  }
  
  // 2. Generate TOTP challenge
  String totpSecret = totpService.generateSecret();
  request.setSession(createTempSession(user, totpSecret));
  
  return ResponseEntity.ok(new AuthResponse(
    status: "MFA_REQUIRED",
    sessionToken: request.getSession(),
    message: "Enter 6-digit code from authenticator app"
  ));
}

@PostMapping("/admin/login/mfa")
public ResponseEntity<AuthResponse> adminLoginMfa(
  @RequestBody AdminMfaRequest request
) {
  // 1. Verify TOTP code
  if (!totpService.verify(request.getTotpCode(), request.getSession().getTotpSecret())) {
    throw new BadCredentialsException("Invalid MFA code");
  }
  
  // 2. Return JWT token (MFA_VERIFIED claim)
  String token = jwtProvider.generateAccessToken(
    request.getSession().getUser(),
    claims: { "mfa_verified": true, "mfa_time": now() }
  );
  
  return ResponseEntity.ok(new AuthResponse(
    accessToken: token,
    tokenType: "Bearer",
    expiresIn: 900,
    mfaVerified: true
  ));
}
```

**Phase 2: Enforce MFA for All Production Access**
```yaml
# application.yml
security:
  mfa:
    required: true
    enabledRoles: [ADMIN, SUPER_ADMIN]
    algorithms: [TOTP]
    issuer: "PII Vault"
    qrCodeExpiry: 300  # seconds
```

**Phase 3: TOTP Recovery Procedures**
- Generate backup codes (10 single-use codes) during MFA setup
- Store backup codes encrypted in database
- Require two backup codes to bypass TOTP temporarily
- Force TOTP reconfiguration on next login

**Evidence Collection**:
- MFA configuration screenshots
- Sample TOTP setup process documentation
- Backup code procedures document
- User MFA enrollment log (with dates)

#### Implement Access Request & Approval Workflow

**Requirement**: All access changes must be approved before provisioning

**Implementation**:
```java
@Entity
public class AccessRequest {
  @Id
  UUID id;
  
  String requesterId;           // Who requested
  String requestorEmail;        // Contact info
  String targetUserId;          // Who gets access
  String targetRole;            // ROLE_USER, ROLE_ADMIN, etc.
  String justification;         // Business reason
  
  AccessRequestStatus status;   // PENDING, APPROVED, REJECTED
  String approverId;            // Who approved
  LocalDateTime approvedAt;
  String approvalComments;
  
  LocalDateTime createdAt;
  LocalDateTime expiresAt;      // Approval expires if not provisioned
  
  @PostPersist
  public void sendApprovalNotification() {
    // Send approval request to manager
    emailService.sendApprovalRequest(
      to: targetUser.getManager().getEmail(),
      subject: "Access Request: " + targetRole,
      body: "justification",
      approveUrl: generateApprovalLink(this.id),
      rejectUrl: generateRejectionLink(this.id)
    );
  }
}

@Service
public class AccessControlService {
  
  public AccessRequest requestAccess(String userId, String role, String justification) {
    AccessRequest req = new AccessRequest(
      targetUserId: userId,
      targetRole: role,
      justification: justification,
      status: PENDING,
      createdAt: now(),
      expiresAt: now().plusDays(7)  // 7-day approval window
    );
    
    repository.save(req);
    auditService.log(OPERATION_TYPE.ACCESS_REQUEST, userId, req.id);
    return req;
  }
  
  public void approveAccess(UUID requestId, String approverId) {
    AccessRequest req = repository.findById(requestId);
    
    // Verify approver has authorization
    if (!hasApprovalAuthority(approverId, req.targetRole)) {
      throw new UnauthorizedException("No approval authority for " + req.targetRole);
    }
    
    req.setStatus(APPROVED);
    req.setApproverId(approverId);
    req.setApprovedAt(now());
    repository.save(req);
    
    // Provision access
    userService.assignRole(req.getTargetUserId(), req.getTargetRole());
    
    auditService.log(OPERATION_TYPE.ACCESS_APPROVED, approverId, req.id);
  }
  
  public void rejectAccess(UUID requestId, String approverId, String reason) {
    AccessRequest req = repository.findById(requestId);
    req.setStatus(REJECTED);
    req.setApproverId(approverId);
    req.setApprovalComments(reason);
    repository.save(req);
    
    // Notify requestor of rejection
    emailService.sendRejection(req.getRequestorEmail(), reason);
    
    auditService.log(OPERATION_TYPE.ACCESS_REJECTED, approverId, req.id);
  }
}
```

**Evidence Collection**:
- Sample access requests (approved, rejected, pending)
- Approval workflow documentation
- Access provisioning log with approval references
- Expiration/cleanup procedures

#### Implement Quarterly Access Reviews

```java
@Entity
public class AccessReview {
  @Id
  UUID id;
  
  String managerId;                    // Manager conducting review
  LocalDateTime reviewPeriodStart;
  LocalDateTime reviewPeriodEnd;
  
  @OneToMany
  List<UserAccessReviewItem> items;   // Items reviewed
  
  AccessReviewStatus status;           // IN_PROGRESS, COMPLETED, CERTIFIED
  LocalDateTime completedAt;
  
  public class UserAccessReviewItem {
    UUID userId;
    Set<String> currentRoles;          // Current access
    String justification;              // Is access still needed?
    ReviewItemStatus status;           // APPROVED, REVOKED, MODIFIED
    LocalDateTime actionTaken;
  }
}

@Service
public class AccessReviewService {
  
  @Scheduled(cron = "0 0 1 * * *")  // 1st of every month
  public void initiateMonthlyAdminReview() {
    Set<User> adminUsers = userService.getUsers(ROLE_ADMIN);
    Set<User> superAdminUsers = userService.getUsers(ROLE_SUPER_ADMIN);
    
    AccessReview review = new AccessReview(
      managerId: ciso.id,
      reviewPeriodStart: now().minusMonths(1).withDayOfMonth(1),
      reviewPeriodEnd: now().withDayOfMonth(1).minusDays(1),
      items: adminUsers.union(superAdminUsers).map(this::createReviewItem)
    );
    
    repository.save(review);
    notificationService.sendReviewNotification(ciso.getEmail(), review.id);
  }
  
  @Scheduled(cron = "0 0 1 * * *")  // Quarterly (Jan 1, Apr 1, Jul 1, Oct 1)
  public void initiateQuarterlyUserReview() {
    Set<User> allUsers = userService.getUsers(ROLE_USER);
    
    Map<String, List<User>> usersByManager = allUsers
      .stream()
      .collect(groupingBy(User::getManagerId));
    
    for (String managerId : usersByManager.keySet()) {
      AccessReview review = new AccessReview(
        managerId: managerId,
        reviewPeriodStart: quarterStart(),
        reviewPeriodEnd: quarterEnd(),
        items: createReviewItems(usersByManager.get(managerId))
      );
      
      repository.save(review);
      notificationService.sendReviewNotification(
        userService.getUser(managerId).getEmail(),
        review.id
      );
    }
  }
  
  public void completeReview(UUID reviewId, List<AccessReviewAction> actions) {
    AccessReview review = repository.findById(reviewId);
    
    for (AccessReviewAction action : actions) {
      UserAccessReviewItem item = review.getItem(action.getUserId());
      
      if (action.getType() == REVOKE) {
        userService.revokeRole(action.getUserId(), action.getRole());
        auditService.log(ACCESS_REVOKED, review.getManagerId(), action.getUserId());
      } else if (action.getType() == MODIFY) {
        userService.assignRole(action.getUserId(), action.getNewRole());
        auditService.log(ACCESS_MODIFIED, review.getManagerId(), action.getUserId());
      }
      
      item.setStatus(action.getStatus());
      item.setActionTaken(now());
    }
    
    review.setStatus(COMPLETED);
    review.setCompletedAt(now());
    repository.save(review);
  }
}
```

**Evidence Collection**:
- Access review initiation logs (monthly and quarterly)
- Completed access review reports
- Access revocation evidence
- Review sign-off documentation

### Week 7-8: Change Management Process

#### Implement Formal Change Management

**Requirement**: All code changes must go through:
1. Code review by peer (security perspective)
2. Security testing (automated + manual)
3. Approval by tech lead before production deploy
4. Change tracking & documentation

**Implementation**:

```java
@Entity
public class Change {
  @Id
  UUID id;
  
  String description;                  // What changed
  String businessJustification;        // Why change needed
  
  String authorId;                     // Who made change (author)
  @CreationTimestamp
  LocalDateTime submittedAt;
  
  String reviewerId;                   // Who reviewed code
  ChangeReviewStatus reviewStatus;     // PENDING, APPROVED, REJECTED
  LocalDateTime reviewedAt;
  String reviewComments;
  
  String approvalId;                   // Who approved deployment
  ChangeApprovalStatus approvalStatus; // PENDING, APPROVED, REJECTED
  LocalDateTime approvedAt;
  String approvalComments;
  
  ChangeStatus status;                 // PENDING, APPROVED, DEPLOYED, ROLLED_BACK
  LocalDateTime deployedAt;
  
  String gitCommitHash;                // Commit hash for traceability
  String gitPullRequestUrl;            // PR link for documentation
  
  @OneToMany
  List<SecurityTestResult> testResults; // Automated security tests
  
  LocalDateTime rollbackDeadline;      // Time after which rollback not possible
  
  public class SecurityTestResult {
    String testName;
    SecurityTestStatus status;         // PASSED, FAILED, SKIPPED
    LocalDateTime executedAt;
    String testDetails;
  }
}

@Service
public class ChangeManagementService {
  
  // 1. Code Review (Pull Request)
  public void submitCodeReview(UUID changeId, String gitPrUrl) {
    Change change = repository.findById(changeId);
    change.setGitPullRequestUrl(gitPrUrl);
    change.setStatus(UNDER_REVIEW);
    repository.save(change);
    
    // Notify reviewers
    notificationService.sendReviewRequest(
      to: getRandomSecurityReviewer().getEmail(),
      subject: "Security Code Review: " + change.getDescription(),
      prUrl: gitPrUrl
    );
  }
  
  // 2. Security Testing (Automated)
  @Scheduled(fixedDelay = 300000)  // Every 5 minutes
  public void runSecurityTests() {
    List<Change> changesUnderReview = repository.findByStatus(UNDER_REVIEW);
    
    for (Change change : changesUnderReview) {
      List<SecurityTestResult> results = runSecurityTestSuite(change.getGitCommitHash());
      
      change.setTestResults(results);
      boolean allPassed = results.stream().allMatch(r -> r.getStatus() == PASSED);
      
      if (!allPassed) {
        notificationService.sendTestFailure(
          to: change.getAuthorId().getEmail(),
          subject: "Security tests failed for change: " + change.getId(),
          results: results
        );
      }
    }
  }
  
  private List<SecurityTestResult> runSecurityTestSuite(String commitHash) {
    return List.of(
      runTest("SAST Analysis", () -> runSonarQube(commitHash)),
      runTest("Dependency Check", () -> runDependencyCheck(commitHash)),
      runTest("Secrets Detection", () -> runSecretsScanning(commitHash)),
      runTest("SQL Injection", () -> runSqlInjectionTests(commitHash)),
      runTest("XSS Vulnerability", () -> runXssTests(commitHash)),
      runTest("Cryptography", () -> validateCryptoImplementation(commitHash))
    );
  }
  
  // 3. Approval Workflow
  public void approveChange(UUID changeId, String reviewerId, String comments) {
    Change change = repository.findById(changeId);
    
    // Verify reviewer has authority
    if (!hasReviewAuthority(reviewerId)) {
      throw new UnauthorizedException("No review authority");
    }
    
    change.setReviewerId(reviewerId);
    change.setReviewStatus(APPROVED);
    change.setReviewComments(comments);
    change.setReviewedAt(now());
    change.setStatus(APPROVED_FOR_DEPLOYMENT);
    repository.save(change);
    
    // Notify author of approval
    notificationService.sendApprovalNotification(
      to: change.getAuthorId().getEmail(),
      subject: "Change approved for deployment: " + change.getId(),
      comments: comments
    );
    
    auditService.log(CHANGE_APPROVED, reviewerId, changeId);
  }
  
  // 4. Deployment
  public void deployChange(UUID changeId, String deployerId) {
    Change change = repository.findById(changeId);
    
    if (change.getStatus() != APPROVED_FOR_DEPLOYMENT) {
      throw new BadStateException("Change not approved for deployment");
    }
    
    // Additional approval for production
    if (isProductionDeployment(change)) {
      if (!hasDeploymentAuthority(deployerId)) {
        throw new UnauthorizedException("No deployment authority");
      }
    }
    
    change.setApprovalId(deployerId);
    change.setApprovalStatus(APPROVED);
    change.setApprovedAt(now());
    change.setDeployedAt(now());
    change.setStatus(DEPLOYED);
    change.setRollbackDeadline(now().plusHours(24));
    repository.save(change);
    
    // Deploy to production
    deploymentService.deploy(change.getGitCommitHash());
    
    auditService.log(CHANGE_DEPLOYED, deployerId, changeId);
    notificationService.sendDeploymentNotification(
      to: allEngineers(),
      subject: "Change deployed: " + change.getId(),
      description: change.getDescription()
    );
  }
  
  // 5. Rollback (if necessary)
  public void rollbackChange(UUID changeId, String rollbackReason) {
    Change change = repository.findById(changeId);
    
    if (now().isAfter(change.getRollbackDeadline())) {
      throw new BadStateException("Rollback deadline exceeded");
    }
    
    deploymentService.rollback(change.getGitCommitHash());
    
    change.setStatus(ROLLED_BACK);
    repository.save(change);
    
    auditService.log(CHANGE_ROLLED_BACK, getCurrentUser().getId(), changeId, rollbackReason);
    notificationService.sendRollbackNotification(allEngineers(), change.getId(), rollbackReason);
  }
  
  // 6. Change Tracking Report
  public ChangeManagementReport generateMonthlyReport(LocalDate month) {
    List<Change> changesInMonth = repository.findByDeployedAtBetween(
      month.atStartOfDay(),
      month.plusMonths(1).atStartOfDay()
    );
    
    return new ChangeManagementReport(
      month: month,
      totalChanges: changesInMonth.size(),
      successfulDeployments: changesInMonth.stream()
        .filter(c -> c.getStatus() == DEPLOYED)
        .count(),
      failedReviews: changesInMonth.stream()
        .filter(c -> c.getReviewStatus() == REJECTED)
        .count(),
      rollbacks: changesInMonth.stream()
        .filter(c -> c.getStatus() == ROLLED_BACK)
        .count(),
      avgTimeToReview: calculateAverageReviewTime(changesInMonth),
      avgTimeToApproval: calculateAverageApprovalTime(changesInMonth),
      securityTestsRun: changesInMonth.stream()
        .flatMap(c -> c.getTestResults().stream())
        .count(),
      securityTestsPassed: changesInMonth.stream()
        .flatMap(c -> c.getTestResults().stream())
        .filter(r -> r.getStatus() == PASSED)
        .count()
    );
  }
}
```

**Change Management Workflow (Visual)**:
```
Code Committed → PR Created → Code Review → Security Tests
                    ↓              ↓              ↓
               Automated       Peer Review   Automated
               by Git          by Engineer   SAST, Secrets
               
                                    ↓
                            Tech Lead Approval
                                    ↓
                            Approved for Deployment
                                    ↓
                            Production Deployment
                                    ↓
                            24-Hour Rollback Window
                                    ↓
                            Deployment Complete
                                    ↓
                        Change Documented in Log
```

**Evidence Collection**:
- Change log for 6-month period (minimum 50-100 changes)
- Sample pull requests with security review comments
- Security test results (automated test reports)
- Approval workflows (screenshots showing approval chain)
- Deployment logs
- Rollback procedures and incident reports (if any)

### Week 9-10: Incident Response & Data Retention

#### Implement Incident Response Procedures

```java
@Entity
public class IncidentReport {
  @Id
  UUID id;
  
  String title;                        // Incident description
  String description;
  
  IncidentSeverity severity;           // CRITICAL, HIGH, MEDIUM, LOW
  IncidentType type;                   // SECURITY, PERFORMANCE, AVAILABILITY, etc.
  
  @CreationTimestamp
  LocalDateTime discoveredAt;          // When incident detected
  LocalDateTime reportedAt;            // When formally reported
  
  String reportedBy;                   // User who reported
  
  IncidentStatus status;               // REPORTED, INVESTIGATING, MITIGATED, RESOLVED, CLOSED
  
  @OneToMany
  List<IncidentInvestigation> investigations;
  
  @OneToMany
  List<IncidentMitigation> mitigations;
  
  LocalDateTime resolvedAt;            // When incident fully resolved
  
  String rootCauseAnalysis;            // RCA document
  String lessonsLearned;               // Preventive measures
  
  String affectedDataElements;         // What data affected
  Integer affectedUserCount;           // How many users affected
  boolean notificationRequired;        // Did we notify customers?
  LocalDateTime notificationSentAt;
  
  public class IncidentInvestigation {
    String investigatorId;
    LocalDateTime startedAt;
    String findings;
    LocalDateTime completedAt;
  }
  
  public class IncidentMitigation {
    String mitigationDescription;
    String ownerId;
    LocalDateTime implementedAt;
    boolean verified;
  }
}

@Service
public class IncidentResponseService {
  
  // 1. Detection & Reporting
  public IncidentReport reportIncident(
    String title,
    String description,
    IncidentSeverity severity
  ) {
    IncidentReport report = new IncidentReport(
      title: title,
      description: description,
      severity: severity,
      reportedAt: now(),
      status: REPORTED
    );
    
    repository.save(report);
    
    // Immediate escalation for CRITICAL incidents
    if (severity == CRITICAL) {
      escalateToOnCall(report);
    } else {
      escalateToSecurityTeam(report);
    }
    
    auditService.log(INCIDENT_REPORTED, getCurrentUser().getId(), report.id);
    return report;
  }
  
  private void escalateToOnCall(IncidentReport report) {
    User onCall = getOnCallEngineer();
    notificationService.sendPagerDutyAlert(
      to: onCall.getPhoneNumber(),
      subject: "CRITICAL INCIDENT: " + report.getTitle(),
      description: report.getDescription()
    );
  }
  
  // 2. Investigation
  public void startInvestigation(UUID incidentId, String investigatorId) {
    IncidentReport report = repository.findById(incidentId);
    
    IncidentInvestigation investigation = new IncidentInvestigation(
      investigatorId: investigatorId,
      startedAt: now()
    );
    
    report.getInvestigations().add(investigation);
    report.setStatus(INVESTIGATING);
    repository.save(report);
    
    auditService.log(INVESTIGATION_STARTED, investigatorId, incidentId);
  }
  
  public void recordInvestigationFindings(
    UUID incidentId,
    String findings
  ) {
    IncidentReport report = repository.findById(incidentId);
    IncidentInvestigation investigation = report.getLatestInvestigation();
    investigation.setFindings(findings);
    investigation.setCompletedAt(now());
    repository.save(report);
  }
  
  // 3. Mitigation
  public void implementMitigation(
    UUID incidentId,
    String mitigationDescription
  ) {
    IncidentReport report = repository.findById(incidentId);
    
    IncidentMitigation mitigation = new IncidentMitigation(
      mitigationDescription: mitigationDescription,
      ownerId: getCurrentUser().getId(),
      implementedAt: now(),
      verified: false
    );
    
    report.getMitigations().add(mitigation);
    report.setStatus(MITIGATED);
    repository.save(report);
    
    auditService.log(MITIGATION_IMPLEMENTED, getCurrentUser().getId(), incidentId);
  }
  
  // 4. Resolution
  public void resolveIncident(
    UUID incidentId,
    String rootCauseAnalysis,
    String lessonsLearned
  ) {
    IncidentReport report = repository.findById(incidentId);
    report.setRootCauseAnalysis(rootCauseAnalysis);
    report.setLessonsLearned(lessonsLearned);
    report.setResolvedAt(now());
    report.setStatus(RESOLVED);
    repository.save(report);
    
    // Schedule post-incident review (blameless postmortem)
    schedulePostIncidentReview(report);
    
    auditService.log(INCIDENT_RESOLVED, getCurrentUser().getId(), incidentId);
  }
  
  // 5. Customer Notification (if data was affected)
  public void notifyCustomersIfRequired(UUID incidentId) {
    IncidentReport report = repository.findById(incidentId);
    
    if (report.getAffectedDataElements().contains("PII") || 
        report.getAffectedUserCount() > 0) {
      
      report.setNotificationRequired(true);
      
      // Generate notification
      String notification = generateDataBreachNotification(
        report.getAffectedDataElements(),
        report.getDescription(),
        report.getMitigations()
      );
      
      // Send notification to affected customers
      sendNotificationToAffectedCustomers(notification);
      
      report.setNotificationSentAt(now());
    }
    
    repository.save(report);
  }
  
  // 6. Incident Trending & Analytics
  public IncidentAnalytics getIncidentAnalytics(LocalDate startDate, LocalDate endDate) {
    List<IncidentReport> incidents = repository.findByReportedAtBetween(
      startDate.atStartOfDay(),
      endDate.atEndOfDay()
    );
    
    return new IncidentAnalytics(
      totalIncidents: incidents.size(),
      byType: incidents.stream()
        .collect(groupingBy(IncidentReport::getType, counting())),
      bySeverity: incidents.stream()
        .collect(groupingBy(IncidentReport::getSeverity, counting())),
      avgResolutionTime: calculateAverageResolutionTime(incidents),
      criticalIncidents: incidents.stream()
        .filter(i -> i.getSeverity() == CRITICAL)
        .count(),
      securityIncidents: incidents.stream()
        .filter(i -> i.getType() == SECURITY)
        .count()
    );
  }
}
```

**Incident Response Playbook Template**:
```markdown
# Security Incident Response Playbook

## Incident Categories & Escalation

### CRITICAL (Response: Immediate)
- Data breach (PII exposed)
- System down (loss of service)
- KMS key compromise
- Ransomware/malware infection

Response:
1. Activate incident commander (on-call engineer)
2. Notify CEO, CISO within 15 minutes
3. Begin investigation immediately
4. Prepare customer notification (within 1 hour if data affected)
5. Page entire security team

### HIGH (Response: 1 hour)
- Unauthorized access attempt
- Potential data exfiltration
- Cryptography vulnerability discovered
- Database integrity issue

Response:
1. Assign investigator within 15 minutes
2. Notify CISO within 30 minutes
3. Begin investigation
4. Prepare mitigation plan

### MEDIUM (Response: 4 hours)
- Failed authentication attempts (above threshold)
- Configuration drift detected
- Policy violation discovered

Response:
1. Log incident formally
2. Notify security team
3. Investigate within 2 hours

### LOW (Response: 24 hours)
- Informational security events
- Policy clarification needed

## Investigation Checklist

- [ ] Timeline of event established
- [ ] System logs collected
- [ ] KMS logs reviewed (if applicable)
- [ ] Database audit logs reviewed
- [ ] Network logs collected
- [ ] Endpoint logs analyzed
- [ ] Affected systems identified
- [ ] Scope of data exposure determined
- [ ] Root cause identified
- [ ] Preventive measures recommended

## Notification (if data affected)

- [ ] Prepare notification within 1 hour
- [ ] Include: What happened, data affected, impact, measures taken
- [ ] Notify customers within 72 hours (varies by jurisdiction)
- [ ] Notify regulatory bodies if required
- [ ] Document all notifications

## Post-Incident Review (Blameless Postmortem)

- [ ] Schedule within 1 week
- [ ] All responders participate
- [ ] Focus on systems, not people
- [ ] Document lessons learned
- [ ] Create preventive action items
- [ ] Track completion of actions
```

**Evidence Collection**:
- Incident response policy document
- Incident log (sample incidents with investigations)
- Post-incident review meeting minutes
- Lessons learned documentation
- Customer notification examples (if any)

#### Implement Data Retention & Destruction Policy

```java
@Entity
public class DataRetention {
  @Id
  String dataClassification;  // PII_ENCRYPTED, AUDIT_LOG, etc.
  
  Integer retentionDays;      // How long to keep
  LocalDate deletionEligibleDate;  // When eligible for deletion
  
  DestructionMethod method;   // CRYPTOGRAPHIC_ERASURE, SHRED, etc.
  String description;
  
  boolean legalHoldApplied;   // Override retention for legal case
}

@Service
public class DataRetentionService {
  
  // Retention Schedule
  public void initializeRetentionPolicies() {
    retentionRepository.save(new DataRetention(
      dataClassification: "ACTIVE_TOKENS",
      retentionDays: 365 * 3,  // 3 years (PCI DSS requirement)
      method: CRYPTOGRAPHIC_ERASURE
    ));
    
    retentionRepository.save(new DataRetention(
      dataClassification: "SOFT_DELETED_TOKENS",
      retentionDays: 365,      // 1 year
      method: CRYPTOGRAPHIC_ERASURE
    ));
    
    retentionRepository.save(new DataRetention(
      dataClassification: "AUDIT_LOGS",
      retentionDays: 365 * 2,  // 2 years (compliance requirement)
      method: ARCHIVAL
    ));
    
    retentionRepository.save(new DataRetention(
      dataClassification: "KMS_LOGS",
      retentionDays: 365 * 2,  // 2 years
      method: ARCHIVAL
    ));
  }
  
  // Scheduled Data Destruction
  @Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
  public void destroyExpiredData() {
    List<DataRetention> policies = retentionRepository.findAll();
    
    for (DataRetention policy : policies) {
      if (policy.isLegalHoldApplied()) {
        continue;  // Skip if legal hold active
      }
      
      List<DataRecord> recordsToDestroy = findRecordsReadyForDestruction(
        policy.getDataClassification(),
        policy.getRetentionDays()
      );
      
      for (DataRecord record : recordsToDestroy) {
        destroyRecord(record, policy.getMethod());
        auditService.log(DATA_DESTROYED, "retention-job", record.id, policy.getMethod());
      }
    }
  }
  
  private void destroyRecord(DataRecord record, DestructionMethod method) {
    if (method == CRYPTOGRAPHIC_ERASURE) {
      // Override key, making data unrecoverable
      record.setEncryptedData("DESTROYED-" + UUID.randomUUID());
      record.setKmsKeyId("DESTROYED");
      record.setDestroyed(true);
      record.setDestroyedAt(now());
      repository.save(record);
      
    } else if (method == ARCHIVAL) {
      // Move to archive storage (S3 Glacier)
      archiveService.archive(record);
      repository.delete(record);
    }
  }
  
  // Legal Hold
  public void applyLegalHold(String dataClassification, String caseId) {
    DataRetention policy = retentionRepository.findById(dataClassification);
    policy.setLegalHoldApplied(true);
    policy.setLegalHoldReference(caseId);
    retentionRepository.save(policy);
    
    auditService.log(LEGAL_HOLD_APPLIED, getCurrentUser().getId(), dataClassification);
  }
  
  public void releaseLegalHold(String dataClassification) {
    DataRetention policy = retentionRepository.findById(dataClassification);
    policy.setLegalHoldApplied(false);
    retentionRepository.save(policy);
    
    auditService.log(LEGAL_HOLD_RELEASED, getCurrentUser().getId(), dataClassification);
  }
  
  // Retention Report
  public DataRetentionReport generateReport(LocalDate reportDate) {
    List<DataRetention> policies = retentionRepository.findAll();
    
    return new DataRetentionReport(
      reportDate: reportDate,
      policies: policies,
      dataDestroyedToday: countDestroyedToday(),
      dataDestroyedMonth: countDestroyedThisMonth(),
      dataDestroyedYear: countDestroyedThisYear(),
      legalHoldsActive: policies.stream()
        .filter(p -> p.isLegalHoldApplied())
        .count()
    );
  }
}
```

**Evidence Collection**:
- Data retention policy document
- Retention policy configuration (showing retention periods)
- Data destruction logs (encrypted records showing data was deleted)
- Legal hold documentation (if applicable)
- Archive procedures and audit trail

### Week 11-12: Disaster Recovery & System Monitoring

#### Implement Disaster Recovery & RTO/RPO Testing

```java
@Entity
public class DisasterRecoveryTest {
  @Id
  UUID id;
  
  String scenarioName;                 // e.g., "Region A data center failure"
  String description;
  
  DisasterRecoveryScenario scenario;   // DATABASE_FAILURE, REGION_FAILURE, etc.
  
  @CreationTimestamp
  LocalDateTime plannedDate;
  LocalDateTime executedAt;
  
  LocalDateTime failoverStartTime;
  LocalDateTime failoverCompletedTime;
  
  long actualRTO_seconds;              // Actual Recovery Time Objective
  long targetRTO_seconds;              // Target (SLA)
  
  LocalDateTime lastDataRecovered;     // Latest data point recovered
  long actualRPO_seconds;              // Actual Recovery Point Objective
  long targetRPO_seconds;              // Target (SLA)
  
  DisasterRecoveryTestStatus status;   // SUCCESS, PARTIAL_SUCCESS, FAILED
  
  String detailedResults;              // Full test report
  String lessonsLearned;               // Improvements needed
  
  @OneToMany
  List<DisasterRecoveryAction> actions; // Actions taken during test
}

@Service
public class DisasterRecoveryService {
  
  // 1. Quarterly DR Test - Region Failure Scenario
  @Scheduled(cron = "0 0 0 1 1,4,7,10 *")  // 1st of Jan, Apr, Jul, Oct
  public void testRegionFailover() {
    DisasterRecoveryTest test = new DisasterRecoveryTest(
      scenarioName: "Region A Failure Simulation",
      scenario: REGION_FAILURE,
      plannedDate: now()
    );
    
    testRepository.save(test);
    auditService.log(DR_TEST_INITIATED, "dr-automation", test.id);
    
    try {
      // Step 1: Cut over to secondary region
      LocalDateTime failoverStart = now();
      test.setFailoverStartTime(failoverStart);
      
      // Stop accepting requests to primary region
      primaryRegionService.stopAcceptingRequests();
      
      // Trigger failover
      secondaryRegionService.startAcceptingRequests();
      
      // Verify all services up in secondary
      boolean allHealthy = verifySecondaryRegionHealth();
      
      if (!allHealthy) {
        throw new DisasterRecoveryException("Secondary region not fully healthy");
      }
      
      LocalDateTime failoverComplete = now();
      test.setFailoverCompletedTime(failoverComplete);
      test.setActualRTO_seconds(
        ChronoUnit.SECONDS.between(failoverStart, failoverComplete)
      );
      
      // Step 2: Verify data integrity
      LocalDateTime lastDataPoint = verifyDataIntegrity();
      test.setLastDataRecovered(lastDataPoint);
      test.setActualRPO_seconds(
        ChronoUnit.SECONDS.between(lastDataPoint, now())
      );
      
      // Step 3: Validate SLA compliance
      if (test.getActualRTO_seconds() <= test.getTargetRTO_seconds() &&
          test.getActualRPO_seconds() <= test.getTargetRPO_seconds()) {
        test.setStatus(SUCCESS);
      } else {
        test.setStatus(PARTIAL_SUCCESS);
      }
      
      // Step 4: Failback (restore primary)
      LocalDateTime failbackStart = now();
      primaryRegionService.restoreFromBackup();
      primaryRegionService.synchronizeWithSecondary();
      primaryRegionService.startAcceptingRequests();
      secondaryRegionService.stopAcceptingRequests();
      
      test.setDetailedResults(generateDRTestReport(test));
      test.setStatus(SUCCESS);
      
    } catch (Exception e) {
      test.setStatus(FAILED);
      test.setDetailedResults("DR Test FAILED: " + e.getMessage());
      
      // Critical - notify team immediately
      notificationService.sendCriticalAlert(
        to: allEngineers(),
        subject: "DR Test FAILED",
        description: e.getMessage()
      );
    }
    
    testRepository.save(test);
    
    // Post-test review
    schedulePostDRReview(test);
  }
  
  // 2. Backup Verification (daily)
  @Scheduled(cron = "0 0 3 * * *")  // 3 AM daily
  public void verifyBackups() {
    List<Backup> recentBackups = backupRepository
      .findByCreatedAtAfter(now().minusHours(25))
      .stream()
      .sorted(comparing(Backup::getCreatedAt).reversed())
      .limit(5)  // Check last 5 backups
      .collect(toList());
    
    for (Backup backup : recentBackups) {
      try {
        // Verify backup file integrity
        long checksumCalculated = calculateChecksum(backup.getBackupFile());
        if (!checksumCalculated.equals(backup.getChecksum())) {
          throw new BackupCorruptedException("Checksum mismatch for " + backup.getId());
        }
        
        // Verify backup can be restored (test restore to isolated environment)
        testRestoreToIsolatedEnv(backup);
        
        backup.setVerified(true);
        backup.setVerificationStatus(SUCCESS);
        
      } catch (Exception e) {
        backup.setVerificationStatus(FAILED);
        backup.setVerificationError(e.getMessage());
        
        // Alert on backup failures
        notificationService.sendAlert(
          to: databaseAdmins(),
          subject: "Backup verification failed for " + backup.getId(),
          description: e.getMessage()
        );
      }
      
      backupRepository.save(backup);
    }
  }
  
  // 3. Restore Procedures
  public void restoreFromBackup(UUID backupId) {
    Backup backup = backupRepository.findById(backupId);
    
    if (!backup.isVerified()) {
      throw new UnverifiedBackupException("Backup not verified");
    }
    
    try {
      // Restore to staging environment first
      databaseService.restoreToStaging(backup);
      
      // Validate data integrity
      validateRestoredData();
      
      // Promote to production (after manual verification)
      auditService.log(BACKUP_RESTORE_INITIATED, getCurrentUser().getId(), backupId);
      
    } catch (Exception e) {
      auditService.log(BACKUP_RESTORE_FAILED, getCurrentUser().getId(), backupId, e.getMessage());
      throw e;
    }
  }
  
  // 4. RTO/RPO SLA Tracking
  public void trackRPOSLA() {
    // Verify replication lag < 1 second (RPO SLA)
    ReplicationLagMetric lag = monitoringService.getReplicationLag();
    
    if (lag.getTotalMillis() > 1000) {
      notificationService.sendAlert(
        to: databaseAdmins(),
        subject: "RPO SLA VIOLATION",
        description: "Replication lag: " + lag.getTotalMillis() + "ms (target: 1000ms)"
      );
    }
  }
}

@Entity
public class Backup {
  @Id
  UUID id;
  
  String backupFile;                   // S3 path or file location
  long fileSizeBytes;
  
  @CreationTimestamp
  LocalDateTime createdAt;
  
  BackupType type;                     // FULL, INCREMENTAL, INCREMENTAL_LOG
  String database;
  
  long checksum;                       // Integrity verification
  boolean verified;
  BackupVerificationStatus verificationStatus;
  String verificationError;
  
  LocalDateTime retentionUntil;        // When to delete backup
  
  @OneToMany
  List<BackupRestoreTest> restoreTests;
}
```

**Evidence Collection**:
- DR test plans and execution logs (quarterly)
- Backup verification logs (daily)
- RTO/RPO measurements
- Restore procedure documentation
- Test results showing data integrity validation

#### Implement System Monitoring & Alerting

```java
@Service
public class MonitoringService {
  
  // Core Metrics
  public void recordMetrics() {
    meterRegistry.timer("encryption.latency").record(() -> {
      // Encrypt operation latency
    });
    
    meterRegistry.counter("encryption.operations.total").increment();
    meterRegistry.counter("decryption.operations.total").increment();
    
    // KMS metrics
    meterRegistry.timer("kms.encrypt.latency").record(() -> {
      // KMS encryption latency
    });
    
    meterRegistry.counter("kms.api.errors").increment();
    
    // Database metrics
    meterRegistry.gauge("database.connections.active", 
      connectionPool.getNumActive());
    
    // Cache metrics
    meterRegistry.counter("cache.hits").increment();
    meterRegistry.counter("cache.misses").increment();
    
    // Security metrics
    meterRegistry.counter("auth.failures").increment();
    meterRegistry.counter("rate.limit.exceeded").increment();
  }
  
  // Alerting Rules
  public void configureAlerts() {
    alertingService.createAlert(
      name: "EncryptionLatencyHigh",
      condition: "encryption.latency.p95 > 50ms",
      severity: "WARNING",
      action: "PagerDuty page on-call engineer"
    );
    
    alertingService.createAlert(
      name: "EncryptionLatencyCritical",
      condition: "encryption.latency.p95 > 100ms",
      severity: "CRITICAL",
      action: "PagerDuty page database team"
    );
    
    alertingService.createAlert(
      name: "KmsApiErrors",
      condition: "kms.api.errors > 1% of requests",
      severity: "CRITICAL",
      action: "PagerDuty page AWS on-call, notify CISO"
    );
    
    alertingService.createAlert(
      name: "AuthenticationFailureSpike",
      condition: "auth.failures > 100 per 5 minutes",
      severity: "CRITICAL",
      action: "Trigger incident response, block suspicious IPs"
    );
    
    alertingService.createAlert(
      name: "DatabaseConnectionPoolExhausted",
      condition: "database.connections.active > 90%",
      severity: "CRITICAL",
      action: "PagerDuty page database admin"
    );
    
    alertingService.createAlert(
      name: "CacheHitRatioDegraded",
      condition: "cache.hit.ratio < 60%",
      severity: "WARNING",
      action: "Notify performance team for tuning"
    );
  }
  
  // Dashboards
  public void setupDashboards() {
    // Operations Dashboard
    grafana.createDashboard("PII-Vault-Operations", panels: [
      panel("Encryption Throughput (ops/sec)", metric: "encryption.operations.total"),
      panel("Decryption Throughput (ops/sec)", metric: "decryption.operations.total"),
      panel("Encryption Latency (p95)", metric: "encryption.latency.p95"),
      panel("Decryption Latency (p95)", metric: "decryption.latency.p95"),
      panel("KMS API Latency", metric: "kms.api.latency"),
      panel("Error Rate (%)", metric: "errors.rate"),
      panel("Database Connections", metric: "database.connections.active"),
      panel("Cache Hit Ratio", metric: "cache.hit.ratio")
    ]);
    
    // Security Dashboard
    grafana.createDashboard("PII-Vault-Security", panels: [
      panel("Authentication Failures", metric: "auth.failures"),
      panel("Failed Login Attempts", metric: "login.failures"),
      panel("Rate Limit Violations", metric: "rate.limit.exceeded"),
      panel("Unauthorized Access Attempts", metric: "unauthorized.access"),
      panel("Admin Operations", metric: "admin.operations.total"),
      panel("Data Access by User", metric: "data.access.by.user"),
      panel("Soft-Deleted Tokens", metric: "tokens.soft.deleted"),
      panel("Security Incidents", metric: "incidents.total")
    ]);
    
    // Infrastructure Dashboard
    grafana.createDashboard("PII-Vault-Infrastructure", panels: [
      panel("CPU Utilization", metric: "cpu.usage.percent"),
      panel("Memory Utilization", metric: "memory.usage.percent"),
      panel("Disk I/O", metric: "disk.io.total"),
      panel("Network I/O", metric: "network.io.total"),
      panel("JVM Heap Usage", metric: "jvm.heap.usage"),
      panel("GC Pause Time", metric: "gc.pause.time.ms"),
      panel("Thread Count", metric: "jvm.thread.count"),
      panel("System Availability", metric: "system.availability.percent")
    ]);
  }
}
```

**Evidence Collection**:
- Monitoring configuration (alerts, thresholds)
- Dashboard screenshots
- Sample alert notifications
- Response procedures to alerts
- SLA tracking (99.95% target)

---

## <a name="observation"></a>Audit Observation Period (Weeks 13-24)

The observation period is the critical 6-month window during which your auditor monitors control effectiveness. During this time:

### What Auditors Do (Weeks 13-24)

1. **Monthly Remote Reviews** (Weeks 13, 17, 21)
   - Auditor reviews control logs (access, changes, incidents)
   - Tests sample transactions (encrypt/decrypt operations)
   - Verifies audit logs are complete and unaltered
   - Reviews incident response for evidence of effectiveness

2. **Quarterly On-Site Visits** (Weeks 16, 20, 24)
   - Interview key personnel (CISO, engineering leads, ops team)
   - Review evidence supporting policies/procedures
   - Observe operational processes in action
   - Inspect infrastructure security controls
   - Verify disaster recovery procedures

3. **Testing Activities**
   - Design effectiveness of controls (do they exist as described?)
   - Operating effectiveness of controls (do they actually work?)
   - Transaction-level testing (sample transactions, verify logging)
   - Data access testing (verify access restrictions enforced)
   - Change management testing (verify approval process followed)

### What You Must Maintain During Observation

```
Week 13-24 Evidence Checklist:

Access Control
├─ [ ] Monthly access review reports
├─ [ ] Access request approvals (every one)
├─ [ ] User provisioning/de-provisioning logs
├─ [ ] MFA enrollment and usage logs
├─ [ ] Failed authentication attempts
└─ [ ] Segregation of duties violations (none expected)

Change Management
├─ [ ] Change log for all 100+ changes deployed
├─ [ ] Code review evidence (all PRs with comments)
├─ [ ] Security test results (automated + manual)
├─ [ ] Change approval workflows
├─ [ ] Deployment logs
└─ [ ] Any rollbacks and reasons

Incident Response
├─ [ ] Incident log (sample incidents)
├─ [ ] Investigation reports
├─ [ ] Mitigation evidence
├─ [ ] Post-incident reviews
└─ [ ] Customer notifications (if any)

Monitoring & Logging
├─ [ ] Audit logs (complete, unaltered)
├─ [ ] KMS usage logs (CloudTrail)
├─ [ ] Database access logs
├─ [ ] System event logs
├─ [ ] Alert logs and responses
└─ [ ] Monitoring dashboard data

Data Protection
├─ [ ] Encryption implementation review
├─ [ ] Key rotation evidence (occurred on schedule)
├─ [ ] Encryption algorithm validation
├─ [ ] TLS certificate management
└─ [ ] Data classification policy enforcement

Disaster Recovery
├─ [ ] Quarterly DR test results
├─ [ ] Backup verification logs (daily)
├─ [ ] RTO/RPO measurements
├─ [ ] Restore procedure documentation
└─ [ ] Any incident recovery evidence

Compliance
├─ [ ] Policy compliance reviews
├─ [ ] Training records (annual security training)
├─ [ ] Vendor risk assessments
├─ [ ] Third-party monitoring evidence
└─ [ ] Regulatory requirement checklist
```

### Critical "Do's" During Observation

✅ **DO**:
- Document EVERYTHING (even mundane operational activities)
- Maintain complete audit logs (never delete or modify)
- Follow all procedures exactly as documented
- Record policy exceptions with justification
- Conduct all scheduled reviews/tests on time
- Respond promptly to auditor inquiries
- Maintain segregation of duties
- Report security incidents immediately
- Test disaster recovery on schedule
- Update evidence in timely manner

### Critical "Don'ts" During Observation

❌ **DON'T**:
- Modify or backdate audit logs
- Approve changes retroactively
- Skip scheduled reviews/tests
- Make exceptions to documented procedures
- Hire staff without going through access workflow
- Deploy code without proper change management
- Disable security controls
- Store plaintext sensitive data
- Share credentials or access among users
- Modify logs after incidents

---

## <a name="auditor"></a>Auditor Engagement & Reporting

### Auditor Selection Criteria

**Key Questions to Ask Auditors**:
1. How many SOC 2 Type II audits have you completed?
2. Do you have SaaS-specific audit experience?
3. What is your audit timeline? (Ideal: 20-week observation + 4-week reporting = 24 weeks)
4. What is your fee structure? Fixed or hourly?
5. Will auditor be available for quarterly on-site visits?
6. What tools/techniques do you use for remote monitoring?
7. Can you provide references from 3-5 similar-sized SaaS companies?
8. Do you have experience with PCI DSS? AWS? Encryption?

### Engagement Structure

**Typical SOC 2 Type II Engagement**:

```
Week 1:  Discovery & Planning
         ├─ Kick-off meeting
         ├─ Scope finalization
         ├─ Auditor resources assigned
         └─ Evidence access provisioned

Week 2-3: Initial Assessment
         ├─ Tour system infrastructure
         ├─ Interview key personnel
         ├─ Review policies & procedures
         └─ Identify control gaps

Week 4:  Observation Period Begins (Week 4)
         ├─ Baseline controls tested
         ├─ Monitoring begins
         └─ Evidence collection protocol established

Week 5-21: Ongoing Monitoring (18 weeks)
          ├─ Monthly remote reviews
          ├─ Quarterly on-site visits
          ├─ Transaction-level testing
          ├─ Incident monitoring
          └─ Continuous evidence collection

Week 22-24: Audit Completion (3 weeks)
           ├─ Final control testing
           ├─ Evidence compilation
           ├─ Auditor interviews final management
           ├─ Draft report prepared
           └─ Exit meeting

Week 25-26: Report Delivery
           ├─ Final SOC 2 Type II report issued
           ├─ Management letter of findings
           └─ Certificate of completion
```

### What's in the Final SOC 2 Type II Report?

**Standard Report Contents** (AAB - Attestation About Availability and Other Service Commitments):

**Executive Summary**
- Organization and scope statement
- Auditor opinion (Unqualified, Qualified, Adverse, Disclaimer)
- Observation period

**Management Responsibility**
- Management's assertion about control effectiveness
- Basis for controls

**Service Auditor Responsibility**
- Auditor opinion statement
- Inherent limitations of controls

**Control Descriptions**
- All controls tested and their design
- Expected results of controls

**Control Testing Results**
- Which controls were effective
- Which controls had deficiencies
- Evidence of operating effectiveness

**Detailed Findings & Observations**
- Any control deficiencies found
- Recommendations for improvement
- Management responses to findings

**Appendix**
- Detailed transaction testing results
- Sample of tested transactions
- Control evidence collected

### Report Types & Certifications

**AAB Report** (AICPA Standard):
- Suitable for SOC 2 Type II
- Covers availability, confidentiality, integrity, privacy
- Signed by AICPA-credentialed auditor

**SSAE 18 Report**:
- Professionally audited attestation
- High-assurance standard
- Often required by large enterprises

**Report Distribution**:
- You own the report (not your customers)
- You share report under NDA with customers
- Report valid for 12 months (needs renewal annually)

---

## <a name="maintenance"></a>Post-Certification Maintenance

### Year 1 Post-Certification

**Q1 - Q2**: After Report Delivered
- [ ] Begin planning for renewal audit (next year)
- [ ] Review all deficiencies from initial audit
- [ ] Implement remediation for any findings
- [ ] Maintain all control evidence (critical!)
- [ ] Continue monitoring and alerting
- [ ] Schedule security training for all staff

**Q3**: Mid-Year Assessment
- [ ] Review control effectiveness (mid-year)
- [ ] Update policies for any regulatory changes
- [ ] Conduct mid-cycle disaster recovery test
- [ ] Review vendor compliance status
- [ ] Assess any new security risks

**Q4**: Renewal Planning
- [ ] Engage auditor for Year 2 audit (start process 2-3 months before observation begins)
- [ ] Update audit scope (any new features/capabilities?)
- [ ] Prepare preliminary evidence
- [ ] Conduct internal control self-assessment
- [ ] Plan for overlapping observation period with Year 2 audit

### Annual Renewal Process

SOC 2 Type II reports are valid for 12 months. You must conduct a new audit annually to maintain certification.

**Renewal Timeline**:
```
Year 2 Audit Timeline:

Month 1-3:    Auditor engagement
              ├─ Kick-off meeting
              ├─ Scope finalization
              └─ Evidence access

Month 4:      Observation begins
              ├─ Baseline controls
              └─ Monitoring established

Month 4-9:    Ongoing monitoring
              ├─ Monthly reviews
              ├─ Quarterly on-site visits
              └─ Transaction testing

Month 10-11:  Audit completion
              ├─ Final testing
              ├─ Draft report
              └─ Exit meeting

Month 12:     Report delivered
              └─ Certificate valid for next 12 months
```

### Continuous Control Improvement

Track control effectiveness metrics:

```java
@Service
public class ControlEffectivenessService {
  
  // Monthly Control Scorecard
  public ControlEffectivenessReport generateMonthlyScorecard() {
    return new ControlEffectivenessReport(
      accessControlsEffectiveness: calculateAccessControlScore(),
      changeManagementEffectiveness: calculateChangeManagementScore(),
      incidentResponseEffectiveness: calculateIncidentResponseScore(),
      monitoringEffectiveness: calculateMonitoringScore(),
      dataProtectionEffectiveness: calculateDataProtectionScore(),
      disasterRecoveryEffectiveness: calculateDRScore(),
      complianceEffectiveness: calculateComplianceScore()
    );
  }
  
  private double calculateAccessControlScore() {
    // Scoring factors:
    // - % of access requests approved vs. rejected
    // - % of access reviews completed on time
    // - % of de-provisioning completed within SLA
    // - # of unauthorized access attempts
    // - # of policy violations
    
    return (approvedAccessRequests / totalAccessRequests) * 0.3 +
           (reviewsOnTime / totalReviews) * 0.2 +
           (deProvisioningOnTime / totalDeProvisioning) * 0.2 +
           (1.0 - (unauthorizedAttempts / totalAccessAttempts)) * 0.15 +
           (1.0 - (policyViolations / totalTransactions)) * 0.15;
  }
}
```

### Common SOC 2 Type II Findings & Remediation

**Finding 1: Incomplete Audit Logs**
- Issue: Some operations not logged
- Remediation: Implement audit logging in all critical code paths
- Evidence: Before/after audit log analysis

**Finding 2: Segregation of Duties**
- Issue: Same person can approve and execute sensitive changes
- Remediation: Implement approval workflow requiring different people
- Evidence: 6 months of changes with separate approvers

**Finding 3: Password Policy Not Enforced**
- Issue: Some users not following password requirements
- Remediation: Enforce via user provisioning system
- Evidence: User creation logs showing enforcement

**Finding 4: Disaster Recovery Testing Infrequent**
- Issue: DR tested less than quarterly
- Remediation: Implement quarterly DR test schedule
- Evidence: 2+ successful DR test executions

**Finding 5: Incident Response Procedures Inadequate**
- Issue: No formal incident handling process
- Remediation: Implement incident response procedures with playbooks
- Evidence: Incident response playbook + 3 example incidents with investigations

---

## Budget & Timeline

### Total SOC 2 Type II Investment

**Costs**:
```
Auditor Fees (Big Firm):       $80,000 - $150,000
Auditor Fees (Boutique Firm):  $40,000 - $80,000
Internal Labor (prep, evidence): $50,000 - $100,000
Tools (monitoring, logging):    $10,000 - $20,000
Policy & Documentation:         $5,000 - $10,000
Training (staff):               $5,000 - $10,000
                                ─────────────────
Estimated Total:              $190,000 - $370,000
```

**Timeline**:
```
Week 1-4:   Pre-audit preparation
Week 5-24:  6-month observation period
Week 25-26: Audit completion & reporting
            ─────────────────────────
Total:      26 weeks (6 months + overhead)
```

### Annual Renewal Costs

- Auditor fees (renewal): $40,000 - $100,000 (typically 20-30% less than initial)
- Internal labor (maintenance): $30,000 - $50,000
- **Total Annual**: $70,000 - $150,000

---

## Success Criteria for SOC 2 Type II

**Certification Achieved When**:
- ✅ Auditor opinion is "Unqualified" (no significant deficiencies)
- ✅ All controls rated "Effective" or "Generally Effective"
- ✅ Zero critical findings
- ✅ All observations have management responses/remediation
- ✅ Report can be shared with customers under NDA
- ✅ Certificate valid for 12 months

**Benefits Upon Certification**:
1. **Sales enablement**: RFP requirement satisfied
2. **Customer confidence**: Third-party validation of security
3. **Competitive advantage**: Market differentiation
4. **Compliance**: Supports PCI DSS, GDPR, HIPAA, etc.
5. **Insurance**: May reduce cyber insurance premiums
6. **Board confidence**: Demonstrates maturity to board/investors

---

## Conclusion

SOC 2 Type II certification is a significant undertaking but essential for enterprise SaaS products. The 6-month observation period ensures controls are not just designed well, but actually working in practice.

**Key Success Factors**:
1. **Engage auditor early** (Week 1, not Week 13)
2. **Plan for 6-month observation** (not 3 months)
3. **Document everything** (controls, policies, evidence)
4. **Follow procedures consistently** (no shortcuts)
5. **Maintain audit logs** (never modify or delete)
6. **Plan for renewals** (annual certification required)
7. **Assign clear ownership** (CISO or Head of Security)

PII Vault's SOC 2 Type II certification will signal to customers that your encryption service is built on a foundation of well-designed and operating security controls—critical for enterprise adoption of sensitive data encryption services.
