package com.civicconnect.admin.views;

import com.civicconnect.admin.model.Report;
import com.civicconnect.admin.service.SupabaseService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;
import java.util.List;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard — CivicConnect Admin")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final SupabaseService supabaseService;

    public DashboardView(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "#F5F5F5");

        buildView();
    }

    private void buildView() {
        // ── Page heading ────────────────────────────────────────────────
        H2 heading = new H2("Dashboard");
        heading.getStyle().set("margin", "0 0 4px").set("color", "#212121");
        Paragraph sub = new Paragraph("Overview of all civic issue reports");
        sub.getStyle().set("color", "#757575").set("margin", "0 0 24px");

        // ── Stat cards ──────────────────────────────────────────────────
        long total      = supabaseService.countAll();
        long reported   = supabaseService.countByStatus("REPORTED");
        long inProgress = supabaseService.countByStatus("IN_PROGRESS");
        long resolved   = supabaseService.countByStatus("RESOLVED");

        HorizontalLayout statsRow = new HorizontalLayout(
            statCard("Total Reports",  String.valueOf(total),      "#1565C0", "#E3F2FD", "📋"),
            statCard("Reported",       String.valueOf(reported),   "#F9A825", "#FFF8E1", "🆕"),
            statCard("In Progress",    String.valueOf(inProgress), "#0288D1", "#E1F5FE", "🔧"),
            statCard("Resolved",       String.valueOf(resolved),   "#2E7D32", "#E8F5E9", "✅")
        );
        statsRow.setWidthFull();
        statsRow.getStyle().set("gap", "16px");
        statsRow.setDefaultVerticalComponentAlignment(Alignment.STRETCH);

        // ── Recent reports grid ──────────────────────────────────────────
        Div tableCard = new Div();
        tableCard.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("padding", "20px")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        H3 tableTitle = new H3("Recent Reports");
        tableTitle.getStyle().set("margin", "0 0 16px").set("color", "#212121");

        List<Report> recent = supabaseService.getAllReports().stream().limit(10).toList();
        Grid<Report> grid = buildMiniGrid(recent);

        tableCard.add(tableTitle, grid);

        add(heading, sub, statsRow, tableCard);
        setWidthFull();
    }

    // ── Stat card helper ─────────────────────────────────────────────────
    private Div statCard(String label, String value, String color, String bg, String emoji) {
        Div card = new Div();
        card.getStyle()
            .set("background", bg)
            .set("border-radius", "12px")
            .set("padding", "20px 24px")
            .set("flex", "1")
            .set("border-left", "4px solid " + color)
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        Span icon = new Span(emoji);
        icon.getStyle().set("font-size", "1.8rem");

        Span num = new Span(value);
        num.getStyle()
            .set("display", "block")
            .set("font-size", "2.4rem")
            .set("font-weight", "700")
            .set("color", color)
            .set("line-height", "1");

        Span lbl = new Span(label);
        lbl.getStyle()
            .set("display", "block")
            .set("font-size", "0.85rem")
            .set("color", "#757575")
            .set("margin-top", "4px");

        card.add(icon, num, lbl);
        return card;
    }

    // ── Mini grid (5 columns, no status editor) ──────────────────────────
    private Grid<Report> buildMiniGrid(List<Report> data) {
        Grid<Report> grid = new Grid<>(Report.class, false);
        grid.setWidthFull();
        grid.setHeight("340px");
        grid.getStyle().set("border", "none");

        grid.addColumn(Report::getTitle).setHeader("Title").setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(Report::getCategory).setHeader("Category").setAutoWidth(true);
        grid.addColumn(Report::getLocationAddress).setHeader("Location").setFlexGrow(1);
        grid.addColumn(r -> statusBadgeText(r.getStatus())).setHeader("Status").setAutoWidth(true);
        grid.addColumn(Report::getShortDate).setHeader("Date").setAutoWidth(true);

        grid.setItems(data);
        return grid;
    }

    private String statusBadgeText(String status) {
        if (status == null) return "REPORTED";
        return switch (status.toUpperCase()) {
            case "IN_PROGRESS" -> "🔧 IN PROGRESS";
            case "RESOLVED"    -> "✅ RESOLVED";
            default            -> "🆕 REPORTED";
        };
    }
}
