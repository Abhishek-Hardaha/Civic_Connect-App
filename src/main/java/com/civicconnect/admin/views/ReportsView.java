package com.civicconnect.admin.views;

import com.civicconnect.admin.model.Report;
import com.civicconnect.admin.service.SupabaseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("All Reports — CivicConnect Admin")
@PermitAll
public class ReportsView extends VerticalLayout {

    private final SupabaseService supabaseService;

    private Grid<Report> grid;
    private List<Report> allReports;

    // Filter controls
    private TextField searchField;
    private Select<String> statusFilter;
    private Select<String> categoryFilter;

    public ReportsView(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        getStyle().set("background", "#F5F5F5");

        buildView();
        loadReports();
    }

    private void buildView() {
        // ── Heading ──────────────────────────────────────────────────────
        H2 heading = new H2("All Reports");
        heading.getStyle().set("margin", "0 0 4px").set("color", "#212121");
        Paragraph sub = new Paragraph("View and manage all civic issue submissions");
        sub.getStyle().set("color", "#757575").set("margin", "0 0 16px");

        // ── Filter bar ───────────────────────────────────────────────────
        searchField = new TextField();
        searchField.setPlaceholder("Search title or location…");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("280px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());

        statusFilter = new Select<>();
        statusFilter.setLabel("Status");
        statusFilter.setItems("ALL", "REPORTED", "IN_PROGRESS", "RESOLVED");
        statusFilter.setValue("ALL");
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> applyFilters());

        categoryFilter = new Select<>();
        categoryFilter.setLabel("Category");
        categoryFilter.setItems("ALL", "ROAD", "POTHOLE", "STREETLIGHT",
                                 "WATER", "ELECTRICITY", "GARBAGE", "OTHER");
        categoryFilter.setValue("ALL");
        categoryFilter.setWidth("150px");
        categoryFilter.addValueChangeListener(e -> applyFilters());

        Button refreshBtn = new Button("↻ Refresh", e -> loadReports());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout filterBar = new HorizontalLayout(
            searchField, statusFilter, categoryFilter, refreshBtn);
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setWidthFull();
        filterBar.setFlexGrow(1, searchField);
        filterBar.getStyle()
            .set("background", "white")
            .set("padding", "16px")
            .set("border-radius", "12px")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        // ── Grid ─────────────────────────────────────────────────────────
        Div tableCard = new Div();
        tableCard.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("padding", "16px")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");
        tableCard.setWidthFull();

        grid = new Grid<>(Report.class, false);
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setHeight("560px");

        // Columns
        grid.addColumn(Report::getTitle)
            .setHeader("Title").setFlexGrow(2).setSortable(true);

        grid.addColumn(Report::getCategory)
            .setHeader("Category").setAutoWidth(true).setSortable(true);

        grid.addColumn(r -> r.getLocationAddress() != null ? r.getLocationAddress() : "—")
            .setHeader("Location").setFlexGrow(1);

        // Status badge column
        grid.addComponentColumn(report -> {
            Span badge = new Span(statusLabel(report.getStatus()));
            badge.getStyle()
                .set("padding", "3px 10px")
                .set("border-radius", "12px")
                .set("font-size", "0.78rem")
                .set("font-weight", "600")
                .set("background", statusBg(report.getStatus()))
                .set("color", statusColor(report.getStatus()));
            return badge;
        }).setHeader("Status").setAutoWidth(true).setSortable(false);

        // Status update dropdown column
        grid.addComponentColumn(report -> {
            Select<String> statusSelect = new Select<>();
            statusSelect.setItems("REPORTED", "IN_PROGRESS", "RESOLVED");
            statusSelect.setValue(report.getStatus() != null ? report.getStatus() : "REPORTED");
            statusSelect.setWidth("145px");
            statusSelect.getStyle().set("font-size", "0.82rem");
            statusSelect.addValueChangeListener(event -> {
                if (!event.getValue().equals(event.getOldValue())) {
                    boolean ok = supabaseService.updateStatus(report.getId(), event.getValue());
                    if (ok) {
                        report.setStatus(event.getValue());
                        Notification n = Notification.show(
                            "Status updated → " + event.getValue(), 3000,
                            Notification.Position.BOTTOM_START);
                        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadReports();  // refresh grid
                    } else {
                        Notification n = Notification.show(
                            "Update failed. Check Supabase connection.", 4000,
                            Notification.Position.BOTTOM_START);
                        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        statusSelect.setValue(event.getOldValue()); // revert
                    }
                }
            });
            return statusSelect;
        }).setHeader("Update Status").setAutoWidth(true);

        grid.addColumn(Report::getShortDate)
            .setHeader("Date").setAutoWidth(true).setSortable(true);

        // Row click → detail dialog
        grid.addItemClickListener(e -> showDetailDialog(e.getItem()));

        tableCard.add(grid);

        add(heading, sub, filterBar, tableCard);
    }

    private void loadReports() {
        allReports = supabaseService.getAllReports();
        applyFilters();
    }

    private void applyFilters() {
        if (allReports == null) return;
        String search   = searchField.getValue() != null ? searchField.getValue().toLowerCase() : "";
        String status   = statusFilter.getValue();
        String category = categoryFilter.getValue();

        List<Report> filtered = allReports.stream()
            .filter(r -> {
                boolean matchSearch = search.isEmpty()
                    || (r.getTitle() != null && r.getTitle().toLowerCase().contains(search))
                    || (r.getLocationAddress() != null && r.getLocationAddress().toLowerCase().contains(search));
                boolean matchStatus = "ALL".equals(status)
                    || status.equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "REPORTED");
                boolean matchCat = "ALL".equals(category)
                    || category.equalsIgnoreCase(r.getCategory());
                return matchSearch && matchStatus && matchCat;
            })
            .collect(Collectors.toList());

        grid.setItems(filtered);
    }

    // ── Detail dialog ────────────────────────────────────────────────────
    private void showDetailDialog(Report report) {
        Dialog dialog = new Dialog();
        dialog.setWidth("560px");
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        H3 title = new H3(report.getTitle());
        title.getStyle().set("margin", "0 0 8px").set("color", "#1565C0");

        Span statusBadge = new Span(statusLabel(report.getStatus()));
        statusBadge.getStyle()
            .set("padding", "4px 12px").set("border-radius", "12px")
            .set("font-weight", "600")
            .set("background", statusBg(report.getStatus()))
            .set("color", statusColor(report.getStatus()));

        Paragraph desc = new Paragraph(report.getDescription() != null
            ? report.getDescription() : "No description");
        desc.getStyle().set("color", "#424242");

        Div details = new Div();
        details.getStyle()
            .set("background", "#F5F5F5").set("border-radius", "8px").set("padding", "12px");
        details.add(
            infoLine("📂 Category", report.getCategory()),
            infoLine("📍 Location", report.getLocationAddress() != null
                ? report.getLocationAddress() : "Not set"),
            infoLine("📅 Submitted", report.getShortDate()),
            infoLine("🆔 ID", report.getId())
        );

        // Photo
        if (report.getPhotoUrl() != null && !report.getPhotoUrl().isEmpty()) {
            Image img = new Image(report.getPhotoUrl(), "Report photo");
            img.setWidth("100%");
            img.getStyle().set("border-radius", "8px").set("margin-top", "8px");
            content.add(title, statusBadge, desc, details, img);
        } else {
            Paragraph noPhoto = new Paragraph("📷 No photo attached");
            noPhoto.getStyle().set("color", "#9E9E9E").set("font-size", "0.85rem");
            content.add(title, statusBadge, desc, details, noPhoto);
        }

        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content, close);
        dialog.open();
    }

    private Div infoLine(String label, String value) {
        Div row = new Div();
        row.getStyle().set("margin-bottom", "6px").set("font-size", "0.88rem");
        Span lbl = new Span(label + ": ");
        lbl.getStyle().set("font-weight", "600").set("color", "#616161");
        Span val = new Span(value != null ? value : "—");
        val.getStyle().set("color", "#212121");
        row.add(lbl, val);
        return row;
    }

    // ── Status helpers ────────────────────────────────────────────────────
    private String statusLabel(String s) {
        if (s == null) return "🆕 REPORTED";
        return switch (s.toUpperCase()) {
            case "IN_PROGRESS" -> "🔧 IN PROGRESS";
            case "RESOLVED"    -> "✅ RESOLVED";
            default            -> "🆕 REPORTED";
        };
    }

    private String statusBg(String s) {
        if (s == null) return "#FFF8E1";
        return switch (s.toUpperCase()) {
            case "IN_PROGRESS" -> "#E1F5FE";
            case "RESOLVED"    -> "#E8F5E9";
            default            -> "#FFF8E1";
        };
    }

    private String statusColor(String s) {
        if (s == null) return "#F9A825";
        return switch (s.toUpperCase()) {
            case "IN_PROGRESS" -> "#0277BD";
            case "RESOLVED"    -> "#2E7D32";
            default            -> "#F9A825";
        };
    }
}
