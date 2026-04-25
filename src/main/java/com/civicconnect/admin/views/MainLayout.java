package com.civicconnect.admin.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authContext;
    private H2 viewTitle;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.getStyle()
            .set("font-size", "1rem")
            .set("margin", "0")
            .set("color", "white");

        // Sign out button
        Button signOut = new Button("Sign out", VaadinIcon.SIGN_OUT.create(), e ->
            authContext.logout());
        signOut.getStyle()
            .set("color", "white")
            .set("cursor", "pointer");
        signOut.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle, signOut);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(viewTitle);
        header.setWidthFull();
        header.getStyle()
            .set("background", "linear-gradient(90deg, #1565C0, #0D47A1)")
            .set("padding", "0 16px");

        addToNavbar(true, header);
    }

    private void createDrawer() {
        // Logo section
        Div logoSection = new Div();
        logoSection.getStyle()
            .set("padding", "20px 16px 16px")
            .set("background", "linear-gradient(135deg, #1565C0, #0D47A1)")
            .set("color", "white");

        H3 appName = new H3("🏛 CivicConnect");
        appName.getStyle()
            .set("margin", "0")
            .set("color", "white")
            .set("font-size", "1.1rem");

        Paragraph role = new Paragraph("Administrator");
        role.getStyle()
            .set("margin", "4px 0 0")
            .set("color", "#90CAF9")
            .set("font-size", "0.8rem");

        logoSection.add(appName, role);

        // Navigation
        SideNav nav = new SideNav();
        nav.getStyle().set("padding", "8px");

        SideNavItem dashboardItem = new SideNavItem("Dashboard",
            DashboardView.class, VaadinIcon.DASHBOARD.create());

        SideNavItem reportsItem = new SideNavItem("All Reports",
            ReportsView.class, VaadinIcon.LIST.create());

        nav.addItem(dashboardItem, reportsItem);

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("drawer-scroller");

        addToDrawer(logoSection, scroller);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value().replace(" — CivicConnect Admin", "");
    }
}
