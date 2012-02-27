/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.ComponentState;
import com.vaadin.terminal.gwt.client.EventHelper;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.communication.ClientToServerRpc;

public class VButtonPaintable extends AbstractComponentConnector {

    /**
     * RPC interface for calls from client to server.
     * 
     * @since 7.0
     */
    public interface ButtonClientToServerRpc extends ClientToServerRpc {
        /**
         * Button click event.
         * 
         * @param mouseEventDetails
         *            serialized mouse event details
         */
        public void click(String mouseEventDetails);

        /**
         * Indicate to the server that the client has disabled the button as a
         * result of a click.
         */
        public void disableOnClick();
    }

    @Override
    protected boolean delegateCaptionHandling() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        ButtonClientToServerRpc rpcProxy = GWT
                .create(ButtonClientToServerRpc.class);
        getWidget().buttonRpcProxy = initRPC(rpcProxy);
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {

        // Ensure correct implementation,
        // but don't let container manage caption etc.
        super.updateFromUIDL(uidl, client);
        if (!isRealUpdate(uidl)) {
            return;
        }

        getWidget().focusHandlerRegistration = EventHelper
                .updateFocusHandler(this, client,
                        getWidget().focusHandlerRegistration);
        getWidget().blurHandlerRegistration = EventHelper
                .updateBlurHandler(this, client,
                        getWidget().blurHandlerRegistration);

        // Save details
        getWidget().client = client;
        getWidget().paintableId = uidl.getId();

        // Set text
        getWidget().setText(getState().getCaption());

        getWidget().disableOnClick = getState().isDisableOnClick();

        // handle error
        if (uidl.hasAttribute("error")) {
            if (getWidget().errorIndicatorElement == null) {
                getWidget().errorIndicatorElement = DOM
                        .createSpan();
                getWidget().errorIndicatorElement
                        .setClassName("v-errorindicator");
            }
            getWidget().wrapper.insertBefore(
                    getWidget().errorIndicatorElement,
                    getWidget().captionElement);

        } else if (getWidget().errorIndicatorElement != null) {
            getWidget().wrapper
                    .removeChild(getWidget().errorIndicatorElement);
            getWidget().errorIndicatorElement = null;
        }

        if (uidl.hasAttribute(ATTRIBUTE_ICON)) {
            if (getWidget().icon == null) {
                getWidget().icon = new Icon(client);
                getWidget().wrapper.insertBefore(
                        getWidget().icon.getElement(),
                        getWidget().captionElement);
            }
            getWidget().icon.setUri(uidl
                    .getStringAttribute(ATTRIBUTE_ICON));
        } else {
            if (getWidget().icon != null) {
                getWidget().wrapper
                        .removeChild(getWidget().icon.getElement());
                getWidget().icon = null;
            }
        }

        getWidget().clickShortcut = getState()
                .getClickShortcutKeyCode();
    }

    @Override
    protected Widget createWidget() {
        return GWT.create(VButton.class);
    }

    @Override
    public VButton getWidget() {
        return (VButton) super.getWidget();
    }

    @Override
    public ButtonState getState() {
        return (ButtonState) super.getState();
    }

    @Override
    protected ComponentState createState() {
        return GWT.create(ButtonState.class);
    }
}
