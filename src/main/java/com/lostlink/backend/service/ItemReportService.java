package com.lostlink.backend.service;

import com.lostlink.backend.dto.item.ItemCreateRequest;
import com.lostlink.backend.dto.item.ItemReportDto;
import com.lostlink.backend.dto.item.ItemUpdateRequest;
import com.lostlink.backend.entity.Category;
import com.lostlink.backend.entity.Claim;
import com.lostlink.backend.entity.ItemMatch;
import com.lostlink.backend.entity.ItemReport;
import com.lostlink.backend.entity.ItemStatus;
import com.lostlink.backend.entity.ReportType;
import com.lostlink.backend.entity.User;
import com.lostlink.backend.exception.ResourceNotFoundException;
import com.lostlink.backend.exception.UnauthorizedException;
import com.lostlink.backend.repository.ClaimRepository;
import com.lostlink.backend.repository.ItemMatchRepository;
import com.lostlink.backend.repository.ItemReportRepository;
import com.lostlink.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemReportService {

    private final ItemReportRepository itemReportRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final MatchingService matchingService;

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    public ItemReportService(
            ItemReportRepository itemReportRepository,
            CategoryService categoryService,
            UserRepository userRepository,
            ClaimRepository claimRepository,
            ItemMatchRepository itemMatchRepository,
            MatchingService matchingService
    ) {
        this.itemReportRepository = itemReportRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.matchingService = matchingService;
    }

    @PostConstruct
    @Transactional
    public void seedInitialRecordsIfEmpty() {
        if (itemReportRepository.count() > 0) {
            if (itemMatchRepository.count() == 0) {
                itemReportRepository.findAll().forEach(matchingService::processMatchesForReport);
            }
            return;
        }

        Category electronics = categoryService.getOrCreateCategory("Electronics");
        Category cards = categoryService.getOrCreateCategory("Student IDs & Cards");
        Category keys = categoryService.getOrCreateCategory("Keys");
        Category books = categoryService.getOrCreateCategory("Books & Stationery");
        Category bags = categoryService.getOrCreateCategory("Clothing & Bags");
        Category accessories = categoryService.getOrCreateCategory("Personal Accessories");

        itemReportRepository.save(ItemReport.builder()
                .title("Apple MacBook Air M2 (Space Gray)")
                .type(ReportType.LOST)
                .category(electronics)
                .description("Left near the quiet study pods on the 2nd floor. Has a small university robotics sticker on the top cover.")
                .location("Main Campus Library, 2nd Floor")
                .itemDate(LocalDate.now().minusDays(1))
                .status(ItemStatus.REPORTED)
                .contactInfo("Submit claim through campus portal")
                .aiConfidenceNote("94% match with found item charger")
                .verifiedBySecurity(false)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Student ID Card — John Doe")
                .type(ReportType.FOUND)
                .category(cards)
                .description("Found on table #14 in the dining hall after the lunch rush. Turned into student security desk.")
                .location("Student Dining Commons")
                .itemDate(LocalDate.now())
                .status(ItemStatus.HELD_AT_DESK)
                .contactInfo("Security Desk, Student Center Rm 104")
                .verifiedBySecurity(true)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Dorm Room Key with Blue Lanyard")
                .type(ReportType.LOST)
                .category(keys)
                .description("Brass room key attached to a navy blue campus lanyard with a small metal whistle charm.")
                .location("Science Complex, Hallway B")
                .itemDate(LocalDate.now().minusDays(2))
                .status(ItemStatus.REPORTED)
                .contactInfo("Campus Security Notification")
                .verifiedBySecurity(false)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Laptop Charger & USB-C Cable")
                .type(ReportType.FOUND)
                .category(electronics)
                .description("White 67W power adapter and braided cable found plugged into wall outlet near the whiteboard.")
                .location("Engineering Building, Rm 302")
                .itemDate(LocalDate.now().minusDays(1))
                .status(ItemStatus.HELD_AT_DESK)
                .contactInfo("Engineering Dept Office, Floor 3")
                .verifiedBySecurity(true)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Graphing Calculator (TI-84 Plus CE)")
                .type(ReportType.LOST)
                .category(electronics)
                .description("Black graphing calculator with initials etched on back battery cover. Misplaced after calculus exam.")
                .location("Mathematics Hall, Lecture Room 1")
                .itemDate(LocalDate.now().minusDays(3))
                .status(ItemStatus.REPORTED)
                .contactInfo("Math Department Reception")
                .verifiedBySecurity(false)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Wireless Earbuds in Black Charging Case")
                .type(ReportType.FOUND)
                .category(accessories)
                .description("Found on the bench in the fitness gym locker area. Currently held at the sports desk.")
                .location("Campus Recreation Center")
                .itemDate(LocalDate.now().minusDays(1))
                .status(ItemStatus.HELD_AT_DESK)
                .contactInfo("Recreation Center Front Desk")
                .verifiedBySecurity(true)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Navy Blue Backpack (North Face)")
                .type(ReportType.LOST)
                .category(bags)
                .description("Contains notebook for organic chemistry and laboratory safety goggles. High sentimental value.")
                .location("Campus Green / Outdoor Amphitheater")
                .itemDate(LocalDate.now().minusDays(4))
                .status(ItemStatus.REPORTED)
                .contactInfo("Campus Security")
                .verifiedBySecurity(false)
                .build());

        itemReportRepository.save(ItemReport.builder()
                .title("Organic Chemistry 4th Edition Textbook")
                .type(ReportType.FOUND)
                .category(books)
                .description("Hardcover textbook with yellow highlights in chapter 4. Found in lecture hall auditorium.")
                .location("Auditorium Hall C")
                .itemDate(LocalDate.now().minusDays(2))
                .status(ItemStatus.HELD_AT_DESK)
                .contactInfo("Auditorium Lost & Found Box")
                .verifiedBySecurity(true)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ItemReportDto> searchItems(String typeStr, String categoryStr, String query, String sortDirection) {
        return searchItems(typeStr, categoryStr, query, sortDirection, false);
    }

    @Transactional(readOnly = true)
    public List<ItemReportDto> searchItems(String typeStr, String categoryStr, String query, String sortDirection, Boolean aiMatchesOnly) {
        ReportType type = null;
        if (typeStr != null && !typeStr.equalsIgnoreCase("all")) {
            try {
                type = ReportType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String category = null;
        if (categoryStr != null && !categoryStr.equalsIgnoreCase("All Categories")) {
            category = categoryStr.trim();
        }

        String search = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        Sort.Direction direction = "oldest".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "itemDate", "id");

        List<ItemReport> reports = itemReportRepository.searchItems(type, category, search, sort);

        if (Boolean.TRUE.equals(aiMatchesOnly)) {
            reports = reports.stream()
                    .filter(r -> r.getAiConfidenceNote() != null && !r.getAiConfidenceNote().isBlank())
                    .collect(Collectors.toList());
        }

        return reports.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItemReportDto> getAllReports() {
        return itemReportRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemReportDto updateReportStatusByAdmin(Long id, String status, Boolean verifiedBySecurity) {
        ItemReport report = itemReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        if (status != null && !status.isBlank()) {
            try {
                report.setStatus(ItemStatus.valueOf(status.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (verifiedBySecurity != null) {
            report.setVerifiedBySecurity(verifiedBySecurity);
        }

        ItemReport saved = itemReportRepository.save(report);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public ItemReportDto getItemById(Long id) {
        ItemReport report = itemReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return mapToDto(report);
    }

    @Transactional
    public ItemReportDto createItem(ItemCreateRequest request, String userEmail) {
        ReportType type = "found".equalsIgnoreCase(request.getType()) ? ReportType.FOUND : ReportType.LOST;
        Category category = categoryService.getOrCreateCategory(request.getCategory());

        LocalDate itemDate = LocalDate.now();
        if (request.getDate() != null && !request.getDate().trim().isEmpty()) {
            try {
                itemDate = LocalDate.parse(request.getDate().trim());
            } catch (Exception ignored) {
            }
        }

        User reporter = null;
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            reporter = userRepository.findByEmail(userEmail.trim().toLowerCase()).orElse(null);
        }

        ItemStatus initialStatus = type == ReportType.FOUND ? ItemStatus.HELD_AT_DESK : ItemStatus.REPORTED;

        // Backend Idempotency Protection: check for duplicate submission within 2 minutes
        java.time.LocalDateTime twoMinutesAgo = java.time.LocalDateTime.now().minusMinutes(2);
        List<ItemReport> recentDuplicates = itemReportRepository.findRecentDuplicates(
                request.getTitle().trim(),
                type,
                request.getLocation().trim(),
                request.getDescription().trim(),
                twoMinutesAgo
        );
        if (!recentDuplicates.isEmpty()) {
            return mapToDto(recentDuplicates.get(0));
        }

        ItemReport report = ItemReport.builder()
                .title(request.getTitle().trim())
                .type(type)
                .category(category)
                .description(request.getDescription().trim())
                .location(request.getLocation().trim())
                .itemDate(itemDate)
                .status(initialStatus)
                .contactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null)
                .reporter(reporter)
                .verifiedBySecurity(type == ReportType.FOUND)
                .build();

        ItemReport saved = itemReportRepository.save(report);
        matchingService.processMatchesForReport(saved);
        return mapToDto(saved);
    }

    @Transactional
    public ItemReportDto updateItem(
            Long id,
            ItemUpdateRequest request,
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        ItemReport report = itemReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        checkAuthorization(report, userEmail, authorities);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            report.setTitle(request.getTitle().trim());
        }
        if (request.getType() != null && !request.getType().trim().isEmpty()) {
            try {
                report.setType(ReportType.valueOf(request.getType().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            report.setCategory(categoryService.getOrCreateCategory(request.getCategory().trim()));
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            report.setLocation(request.getLocation().trim());
        }
        if (request.getDate() != null && !request.getDate().trim().isEmpty()) {
            try {
                report.setItemDate(LocalDate.parse(request.getDate().trim()));
            } catch (Exception ignored) {}
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            report.setDescription(request.getDescription().trim());
        }
        if (request.getContactInfo() != null) {
            report.setContactInfo(request.getContactInfo().trim());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                report.setStatus(ItemStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        ItemReport saved = itemReportRepository.save(report);
        matchingService.processMatchesForReport(saved);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteItem(
            Long id,
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        ItemReport report = itemReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        checkAuthorization(report, userEmail, authorities);

        // Remove referenced claims and matches first to respect referential integrity
        List<Claim> claims = claimRepository.findByItemId(report.getId());
        if (!claims.isEmpty()) {
            claimRepository.deleteAll(claims);
        }
        List<ItemMatch> lostMatches = itemMatchRepository.findByLostItemId(report.getId());
        if (!lostMatches.isEmpty()) {
            itemMatchRepository.deleteAll(lostMatches);
        }
        List<ItemMatch> foundMatches = itemMatchRepository.findByFoundItemId(report.getId());
        if (!foundMatches.isEmpty()) {
            itemMatchRepository.deleteAll(foundMatches);
        }

        itemReportRepository.delete(report);
    }

    private void checkAuthorization(
            ItemReport report,
            String userEmail,
            Collection<? extends GrantedAuthority> authorities
    ) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required to manage this report");
        }

        boolean isAdminOrStaff = authorities != null && authorities.stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF")
        );

        boolean isOwner = report.getReporter() != null &&
                report.getReporter().getEmail().equalsIgnoreCase(userEmail.trim());

        if (!isAdminOrStaff && !isOwner) {
            throw new AccessDeniedException("You are not authorized to modify or delete this report");
        }
    }

    public ItemReportDto mapToDto(ItemReport report) {
        String formattedDate = report.getItemDate() != null ? report.getItemDate().format(DISPLAY_FORMATTER) : "";
        String statusLabel = report.getStatus() == ItemStatus.HELD_AT_DESK ? "Held at Helpdesk" :
                             report.getStatus() == ItemStatus.CLAIMED ? "Claimed" :
                             report.getStatus() == ItemStatus.RESOLVED ? "Resolved" : "Reported";

        return ItemReportDto.builder()
                .id(report.getId())
                .title(report.getTitle())
                .description(report.getDescription())
                .type(report.getType() != null ? report.getType().name().toLowerCase() : "lost")
                .category(report.getCategory() != null ? report.getCategory().getName() : "Others")
                .location(report.getLocation())
                .date(report.getItemDate() != null ? report.getItemDate().toString() : "")
                .formattedDate(formattedDate)
                .status(statusLabel)
                .contactInfo(report.getContactInfo())
                .securityVerified(report.isVerifiedBySecurity())
                .aiConfidence(report.getAiConfidenceNote())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                .build();
    }
}
