package com.civicconnect.admin.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login — CivicConnect Admin")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)");

        // Card container
        Div card = new Div();
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "16px")
            .set("padding", "40px")
            .set("box-shadow", "0 20px 60px rgba(0,0,0,0.3)")
            .set("width", "400px")
            .set("max-width", "90vw");

        // Header
        H1 title = new H1("🏛 CivicConnect");
        title.getStyle()
            .set("color", "#1565C0")
            .set("margin", "0 0 4px 0")
            .set("font-size", "1.8rem");

        H3 subtitle = new H3("Admin Dashboard");
        subtitle.getStyle()
            .set("color", "#757575")
            .set("margin", "0 0 24px 0")
            .set("font-weight", "400")
            .set("font-size", "1rem");

        Paragraph hint = new Paragraph("Sign in to manage civic issue reports");
        hint.getStyle().set("color", "#9E9E9E").set("font-size", "0.85rem").set("margin", "0 0 16px 0");

        loginForm.setAction("login");
        loginForm.getStyle().set("width", "100%");

        card.add(title, subtitle, hint, loginForm);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters()
                .getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
