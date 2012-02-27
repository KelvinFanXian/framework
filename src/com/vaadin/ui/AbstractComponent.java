/* 
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.ui;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.Application;
import com.vaadin.event.ActionManager;
import com.vaadin.event.EventRouter;
import com.vaadin.event.MethodEventSource;
import com.vaadin.event.ShortcutListener;
import com.vaadin.terminal.ErrorMessage;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.PaintTarget.PaintStatus;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.gwt.client.ComponentState;
import com.vaadin.terminal.gwt.client.ui.AbstractComponentConnector;
import com.vaadin.terminal.gwt.server.ComponentSizeValidator;
import com.vaadin.terminal.gwt.server.RpcManager;
import com.vaadin.terminal.gwt.server.RpcTarget;
import com.vaadin.terminal.gwt.server.ServerRpcManager;
import com.vaadin.tools.ReflectTools;

/**
 * An abstract class that defines default implementation for the
 * {@link Component} interface. Basic UI components that are not derived from an
 * external component can inherit this class to easily qualify as Vaadin
 * components. Most components in Vaadin do just that.
 * 
 * @author Vaadin Ltd.
 * @version
 * @VERSION@
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractComponent implements Component, MethodEventSource {

    /* Private members */

    /**
     * Style names.
     */
    private ArrayList<String> styles;

    /**
     * Caption text.
     */
    private String caption;

    /**
     * Application specific data object. The component does not use or modify
     * this.
     */
    private Object applicationData;

    /**
     * Icon to be shown together with caption.
     */
    private Resource icon;

    /**
     * Is the component enabled (its normal usage is allowed).
     */
    private boolean enabled = true;

    /**
     * Is the component visible (it is rendered).
     */
    private boolean visible = true;

    /**
     * Is the component read-only ?
     */
    private boolean readOnly = false;

    /**
     * Description of the usage (XML).
     */
    private String description = null;

    /**
     * The container this component resides in.
     */
    private Component parent = null;

    /**
     * The EventRouter used for the event model.
     */
    private EventRouter eventRouter = null;

    /**
     * A set of event identifiers with registered listeners.
     */
    private Set<String> eventIdentifiers = null;

    /**
     * The internal error message of the component.
     */
    private ErrorMessage componentError = null;

    /**
     * Immediate mode: if true, all variable changes are required to be sent
     * from the terminal immediately.
     */
    private boolean immediate = false;

    /**
     * Locale of this component.
     */
    private Locale locale;

    /**
     * The component should receive focus (if {@link Focusable}) when attached.
     */
    private boolean delayedFocus;

    /**
     * List of repaint request listeners or null if not listened at all.
     */
    private LinkedList<RepaintRequestListener> repaintRequestListeners = null;

    /**
     * Are all the repaint listeners notified about recent changes ?
     */
    private boolean repaintRequestListenersNotified = false;

    private String testingId;

    /* Sizeable fields */

    private float width = SIZE_UNDEFINED;
    private float height = SIZE_UNDEFINED;
    private Unit widthUnit = Unit.PIXELS;
    private Unit heightUnit = Unit.PIXELS;
    private static final Pattern sizePattern = Pattern
            .compile("^(-?\\d+(\\.\\d+)?)(%|px|em|ex|in|cm|mm|pt|pc)?$");

    private ComponentErrorHandler errorHandler = null;

    /**
     * Keeps track of the Actions added to this component; the actual
     * handling/notifying is delegated, usually to the containing window.
     */
    private ActionManager actionManager;

    /**
     * A map from RPC interface class to the RPC call manager that handles
     * incoming RPC calls for that interface.
     */
    private Map<Class<?>, RpcManager> rpcManagerMap = new HashMap<Class<?>, RpcManager>();

    /**
     * Shared state object to be communicated from the server to the client when
     * modified.
     */
    private ComponentState sharedState;

    /* Constructor */

    /**
     * Constructs a new Component.
     */
    public AbstractComponent() {
        // ComponentSizeValidator.setCreationLocation(this);
    }

    /* Get/Set component properties */

    public void setDebugId(String id) {
        testingId = id;
    }

    public String getDebugId() {
        return testingId;
    }

    /**
     * Gets style for component. Multiple styles are joined with spaces.
     * 
     * @return the component's styleValue of property style.
     * @deprecated Use getStyleName() instead; renamed for consistency and to
     *             indicate that "style" should not be used to switch client
     *             side implementation, only to style the component.
     */
    @Deprecated
    public String getStyle() {
        return getStyleName();
    }

    /**
     * Sets and replaces all previous style names of the component. This method
     * will trigger a {@link com.vaadin.terminal.Paintable.RepaintRequestEvent
     * RepaintRequestEvent}.
     * 
     * @param style
     *            the new style of the component.
     * @deprecated Use setStyleName() instead; renamed for consistency and to
     *             indicate that "style" should not be used to switch client
     *             side implementation, only to style the component.
     */
    @Deprecated
    public void setStyle(String style) {
        setStyleName(style);
    }

    /*
     * Gets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    public String getStyleName() {
        String s = "";
        if (styles != null) {
            for (final Iterator<String> it = styles.iterator(); it.hasNext();) {
                s += it.next();
                if (it.hasNext()) {
                    s += " ";
                }
            }
        }
        return s;
    }

    /*
     * Sets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    public void setStyleName(String style) {
        if (style == null || "".equals(style)) {
            styles = null;
            requestRepaint();
            return;
        }
        if (styles == null) {
            styles = new ArrayList<String>();
        }
        styles.clear();
        String[] styleParts = style.split(" +");
        for (String part : styleParts) {
            if (part.length() > 0) {
                styles.add(part);
            }
        }
        requestRepaint();
    }

    public void addStyleName(String style) {
        if (style == null || "".equals(style)) {
            return;
        }
        if (styles == null) {
            styles = new ArrayList<String>();
        }
        if (!styles.contains(style)) {
            styles.add(style);
            requestRepaint();
        }
    }

    public void removeStyleName(String style) {
        if (styles != null) {
            String[] styleParts = style.split(" +");
            for (String part : styleParts) {
                if (part.length() > 0) {
                    styles.remove(part);
                }
            }
            requestRepaint();
        }
    }

    /*
     * Get's the component's caption. Don't add a JavaDoc comment here, we use
     * the default documentation from implemented interface.
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the component's caption <code>String</code>. Caption is the visible
     * name of the component. This method will trigger a
     * {@link com.vaadin.terminal.Paintable.RepaintRequestEvent
     * RepaintRequestEvent}.
     * 
     * @param caption
     *            the new caption <code>String</code> for the component.
     */
    public void setCaption(String caption) {
        this.caption = caption;
        requestRepaint();
    }

    /*
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    public Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        if (parent != null) {
            return parent.getLocale();
        }
        final Application app = getApplication();
        if (app != null) {
            return app.getLocale();
        }
        return null;
    }

    /**
     * Sets the locale of this component.
     * 
     * <pre>
     * // Component for which the locale is meaningful
     * InlineDateField date = new InlineDateField(&quot;Datum&quot;);
     * 
     * // German language specified with ISO 639-1 language
     * // code and ISO 3166-1 alpha-2 country code.
     * date.setLocale(new Locale(&quot;de&quot;, &quot;DE&quot;));
     * 
     * date.setResolution(DateField.RESOLUTION_DAY);
     * layout.addComponent(date);
     * </pre>
     * 
     * 
     * @param locale
     *            the locale to become this component's locale.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;

        // FIXME: Reload value if there is a converter
        requestRepaint();
    }

    /*
     * Gets the component's icon resource. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    public Resource getIcon() {
        return icon;
    }

    /**
     * Sets the component's icon. This method will trigger a
     * {@link com.vaadin.terminal.Paintable.RepaintRequestEvent
     * RepaintRequestEvent}.
     * 
     * @param icon
     *            the icon to be shown with the component's caption.
     */
    public void setIcon(Resource icon) {
        this.icon = icon;
        requestRepaint();
    }

    /*
     * Tests if the component is enabled or not. Don't add a JavaDoc comment
     * here, we use the default documentation from implemented interface.
     */
    public boolean isEnabled() {
        return enabled && (parent == null || parent.isEnabled()) && isVisible();
    }

    /*
     * Enables or disables the component. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            boolean wasEnabled = this.enabled;
            boolean wasEnabledInContext = isEnabled();

            this.enabled = enabled;

            boolean isEnabled = enabled;
            boolean isEnabledInContext = isEnabled();

            // If the actual enabled state (as rendered, in context) has not
            // changed we do not need to repaint except if the parent is
            // invisible.
            // If the parent is invisible we must request a repaint so the
            // component is repainted with the new enabled state when the parent
            // is set visible again. This workaround is needed as isEnabled
            // checks isVisible.
            boolean needRepaint = (wasEnabledInContext != isEnabledInContext)
                    || (wasEnabled != isEnabled && (getParent() == null || !getParent()
                            .isVisible()));

            if (needRepaint) {
                requestRepaint();
            }
        }
    }

    /*
     * Tests if the component is in the immediate mode. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    public boolean isImmediate() {
        return immediate;
    }

    /**
     * Sets the component's immediate mode to the specified status. This method
     * will trigger a {@link com.vaadin.terminal.Paintable.RepaintRequestEvent
     * RepaintRequestEvent}.
     * 
     * @param immediate
     *            the boolean value specifying if the component should be in the
     *            immediate mode after the call.
     * @see Component#isImmediate()
     */
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
        requestRepaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#isVisible()
     */
    public boolean isVisible() {
        return visible && (getParent() == null || getParent().isVisible());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#setVisible(boolean)
     */
    public void setVisible(boolean visible) {

        if (this.visible != visible) {
            this.visible = visible;
            // Instead of requesting repaint normally we
            // fire the event directly to assure that the
            // event goes through event in the component might
            // now be invisible
            fireRequestRepaintEvent(null);
        }
    }

    /**
     * <p>
     * Gets the component's description, used in tooltips and can be displayed
     * directly in certain other components such as forms. The description can
     * be used to briefly describe the state of the component to the user. The
     * description string may contain certain XML tags:
     * </p>
     * 
     * <p>
     * <table border=1>
     * <tr>
     * <td width=120><b>Tag</b></td>
     * <td width=120><b>Description</b></td>
     * <td width=120><b>Example</b></td>
     * </tr>
     * <tr>
     * <td>&lt;b></td>
     * <td>bold</td>
     * <td><b>bold text</b></td>
     * </tr>
     * <tr>
     * <td>&lt;i></td>
     * <td>italic</td>
     * <td><i>italic text</i></td>
     * </tr>
     * <tr>
     * <td>&lt;u></td>
     * <td>underlined</td>
     * <td><u>underlined text</u></td>
     * </tr>
     * <tr>
     * <td>&lt;br></td>
     * <td>linebreak</td>
     * <td>N/A</td>
     * </tr>
     * <tr>
     * <td>&lt;ul><br>
     * &lt;li>item1<br>
     * &lt;li>item1<br>
     * &lt;/ul></td>
     * <td>item list</td>
     * <td>
     * <ul>
     * <li>item1
     * <li>item2
     * </ul>
     * </td>
     * </tr>
     * </table>
     * </p>
     * 
     * <p>
     * These tags may be nested.
     * </p>
     * 
     * @return component's description <code>String</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the component's description. See {@link #getDescription()} for more
     * information on what the description is. This method will trigger a
     * {@link com.vaadin.terminal.Paintable.RepaintRequestEvent
     * RepaintRequestEvent}.
     * 
     * The description is displayed as HTML/XHTML in tooltips or directly in
     * certain components so care should be taken to avoid creating the
     * possibility for HTML injection and possibly XSS vulnerabilities.
     * 
     * @param description
     *            the new description string for the component.
     */
    public void setDescription(String description) {
        this.description = description;
        requestRepaint();
    }

    /*
     * Gets the component's parent component. Don't add a JavaDoc comment here,
     * we use the default documentation from implemented interface.
     */
    public Component getParent() {
        return parent;
    }

    /*
     * Sets the parent component. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    public void setParent(Component parent) {

        // If the parent is not changed, don't do anything
        if (parent == this.parent) {
            return;
        }

        if (parent != null && this.parent != null) {
            throw new IllegalStateException(getClass().getName()
                    + " already has a parent.");
        }

        // Send detach event if the component have been connected to a window
        if (getApplication() != null) {
            detach();
        }

        // Connect to new parent
        this.parent = parent;

        // Send attach event if connected to a window
        if (getApplication() != null) {
            attach();
        }
    }

    /**
     * Gets the error message for this component.
     * 
     * @return ErrorMessage containing the description of the error state of the
     *         component or null, if the component contains no errors. Extending
     *         classes should override this method if they support other error
     *         message types such as validation errors or buffering errors. The
     *         returned error message contains information about all the errors.
     */
    public ErrorMessage getErrorMessage() {
        return componentError;
    }

    /**
     * Gets the component's error message.
     * 
     * @link Terminal.ErrorMessage#ErrorMessage(String, int)
     * 
     * @return the component's error message.
     */
    public ErrorMessage getComponentError() {
        return componentError;
    }

    /**
     * Sets the component's error message. The message may contain certain XML
     * tags, for more information see
     * 
     * @link Component.ErrorMessage#ErrorMessage(String, int)
     * 
     * @param componentError
     *            the new <code>ErrorMessage</code> of the component.
     */
    public void setComponentError(ErrorMessage componentError) {
        this.componentError = componentError;
        fireComponentErrorEvent();
        requestRepaint();
    }

    /*
     * Tests if the component is in read-only mode. Don't add a JavaDoc comment
     * here, we use the default documentation from implemented interface.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /*
     * Sets the component's read-only mode. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        requestRepaint();
    }

    /*
     * Gets the parent window of the component. Don't add a JavaDoc comment
     * here, we use the default documentation from implemented interface.
     */
    public Root getRoot() {
        if (parent == null) {
            return null;
        } else {
            return parent.getRoot();
        }
    }

    /*
     * Notify the component that it's attached to a window. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    public void attach() {
        requestRepaint();
        if (!visible) {
            /*
             * Bypass the repaint optimization in childRequestedRepaint method
             * when attaching. When reattaching (possibly moving) -> must
             * repaint
             */
            fireRequestRepaintEvent(null);
        }
        if (delayedFocus) {
            focus();
        }
        if (actionManager != null) {
            actionManager.setViewer(getRoot());
        }
    }

    /*
     * Detach the component from application. Don't add a JavaDoc comment here,
     * we use the default documentation from implemented interface.
     */
    public void detach() {
        if (actionManager != null) {
            actionManager.setViewer((Root) null);
        }
    }

    /**
     * Sets the focus for this component if the component is {@link Focusable}.
     */
    protected void focus() {
        if (this instanceof Focusable) {
            final Application app = getApplication();
            if (app != null) {
                getRoot().setFocusedComponent((Focusable) this);
                delayedFocus = false;
            } else {
                delayedFocus = true;
            }
        }
    }

    /**
     * Gets the application object to which the component is attached.
     * 
     * <p>
     * The method will return {@code null} if the component is not currently
     * attached to an application. This is often a problem in constructors of
     * regular components and in the initializers of custom composite
     * components. A standard workaround is to move the problematic
     * initialization to {@link #attach()}, as described in the documentation of
     * the method.
     * </p>
     * <p>
     * <b>This method is not meant to be overridden. Due to CDI requirements we
     * cannot declare it as final even though it should be final.</b>
     * </p>
     * 
     * @return the parent application of the component or <code>null</code>.
     * @see #attach()
     */
    public Application getApplication() {
        if (parent == null) {
            return null;
        } else {
            return parent.getApplication();
        }
    }

    /* Component painting */

    /* Documented in super interface */
    public void requestRepaintRequests() {
        repaintRequestListenersNotified = false;
    }

    /**
     * 
     * <p>
     * Paints the Paintable into a UIDL stream. This method creates the UIDL
     * sequence describing it and outputs it to the given UIDL stream.
     * </p>
     * 
     * <p>
     * It is called when the contents of the component should be painted in
     * response to the component first being shown or having been altered so
     * that its visual representation is changed.
     * </p>
     * 
     * <p>
     * <b>Do not override this to paint your component.</b> Override
     * {@link #paintContent(PaintTarget)} instead.
     * </p>
     * 
     * 
     * @param target
     *            the target UIDL stream where the component should paint itself
     *            to.
     * @throws PaintException
     *             if the paint operation failed.
     */
    public void paint(PaintTarget target) throws PaintException {
        final String tag = target.getTag(this);
        final PaintStatus status = target.startPaintable(this, tag);
        if (PaintStatus.DEFER == status) {
            // nothing to do but flag as deferred and close the paintable tag
            // paint() will be called again later to paint the contents
            target.addAttribute("deferred", true);
        } else if (PaintStatus.CACHED == status
                && !repaintRequestListenersNotified) {
            // Contents have not changed, only cached presentation can be used
            target.addAttribute("cached", true);
        } else {
            // Paint the contents of the component

            // Only paint content of visible components.
            if (isVisible()) {
                // width and height are only in shared state

                // TODO probably can remove also icon once all the VCaption
                // related code has been updated
                if (getIcon() != null) {
                    target.addAttribute(
                            AbstractComponentConnector.ATTRIBUTE_ICON, getIcon());
                }

                if (eventIdentifiers != null) {
                    target.addAttribute("eventListeners",
                            eventIdentifiers.toArray());
                }

                paintContent(target);

                final ErrorMessage error = getErrorMessage();
                if (error != null) {
                    error.paint(target);
                }
            } else {
                target.addAttribute("invisible", true);
            }
        }
        target.endPaintable(this);

        repaintRequestListenersNotified = false;
    }

    /**
     * Build CSS compatible string representation of height.
     * 
     * @return CSS height
     */
    private String getCSSHeight() {
        if (getHeightUnits() == Unit.PIXELS) {
            return ((int) getHeight()) + getHeightUnits().getSymbol();
        } else {
            return getHeight() + getHeightUnits().getSymbol();
        }
    }

    /**
     * Build CSS compatible string representation of width.
     * 
     * @return CSS width
     */
    private String getCSSWidth() {
        if (getWidthUnits() == Unit.PIXELS) {
            return ((int) getWidth()) + getWidthUnits().getSymbol();
        } else {
            return getWidth() + getWidthUnits().getSymbol();
        }
    }

    /**
     * Paints any needed component-specific things to the given UIDL stream. The
     * more general {@link #paint(PaintTarget)} method handles all general
     * attributes common to all components, and it calls this method to paint
     * any component-specific attributes to the UIDL stream.
     * 
     * @param target
     *            the target UIDL stream where the component should paint itself
     *            to
     * @throws PaintException
     *             if the paint operation failed.
     */
    public void paintContent(PaintTarget target) throws PaintException {

    }

    /**
     * Returns the shared state bean with information to be sent from the server
     * to the client.
     * 
     * Subclasses should override this method and set any relevant fields of the
     * state returned by super.getState().
     * 
     * @since 7.0
     * 
     * @return updated component shared state
     */
    public ComponentState getState() {
        if (null == sharedState) {
            sharedState = createState();
        }
        // basic state: caption, size, enabled, ...

        if (!isVisible()) {
            return null;
        }

        // TODO for now, this superclass always recreates the state from
        // scratch, whereas subclasses should only modify it

        if (getHeight() >= 0
                && (getHeightUnits() != Unit.PERCENTAGE || ComponentSizeValidator
                        .parentCanDefineHeight(this))) {
            sharedState.setHeight("" + getCSSHeight());
        }

        if (getWidth() >= 0
                && (getWidthUnits() != Unit.PERCENTAGE || ComponentSizeValidator
                        .parentCanDefineWidth(this))) {
            sharedState.setWidth("" + getCSSWidth());
        }

        sharedState.setImmediate(isImmediate());
        sharedState.setReadOnly(isReadOnly());
        sharedState.setDisabled(!isEnabled());

        sharedState.setStyle(getStyleName());

        sharedState.setCaption(getCaption());
        sharedState.setDescription(getDescription());

        // TODO icon also in shared state - how to convert Resource?

        return sharedState;
    }

    /**
     * Creates the shared state bean to be used in server to client
     * communication.
     * 
     * Subclasses should implement this method and return a new instance of the
     * correct state class.
     * 
     * All configuration of the values of the state should be performed in
     * {@link #getState()}, not in {@link #createState()}.
     * 
     * @since 7.0
     * 
     * @return new shared state object
     */
    protected ComponentState createState() {
        return new ComponentState();
    }

    /* Documentation copied from interface */
    public void requestRepaint() {

        // The effect of the repaint request is identical to case where a
        // child requests repaint
        childRequestedRepaint(null);
    }

    /* Documentation copied from interface */
    public void childRequestedRepaint(
            Collection<RepaintRequestListener> alreadyNotified) {
        // Invisible components (by flag in this particular component) do not
        // need repaints
        if (!visible) {
            return;
        }

        fireRequestRepaintEvent(alreadyNotified);
    }

    /**
     * Fires the repaint request event.
     * 
     * @param alreadyNotified
     */
    private void fireRequestRepaintEvent(
            Collection<RepaintRequestListener> alreadyNotified) {
        // Notify listeners only once
        if (!repaintRequestListenersNotified) {
            // Notify the listeners
            if (repaintRequestListeners != null
                    && !repaintRequestListeners.isEmpty()) {
                final Object[] listeners = repaintRequestListeners.toArray();
                final RepaintRequestEvent event = new RepaintRequestEvent(this);
                for (int i = 0; i < listeners.length; i++) {
                    if (alreadyNotified == null) {
                        alreadyNotified = new LinkedList<RepaintRequestListener>();
                    }
                    if (!alreadyNotified.contains(listeners[i])) {
                        ((RepaintRequestListener) listeners[i])
                                .repaintRequested(event);
                        alreadyNotified
                                .add((RepaintRequestListener) listeners[i]);
                        repaintRequestListenersNotified = true;
                    }
                }
            }

            // Notify the parent
            final Component parent = getParent();
            if (parent != null) {
                parent.childRequestedRepaint(alreadyNotified);
            }
        }
    }

    /* Documentation copied from interface */
    public void addListener(RepaintRequestListener listener) {
        if (repaintRequestListeners == null) {
            repaintRequestListeners = new LinkedList<RepaintRequestListener>();
        }
        if (!repaintRequestListeners.contains(listener)) {
            repaintRequestListeners.add(listener);
        }
    }

    /* Documentation copied from interface */
    public void removeListener(RepaintRequestListener listener) {
        if (repaintRequestListeners != null) {
            repaintRequestListeners.remove(listener);
            if (repaintRequestListeners.isEmpty()) {
                repaintRequestListeners = null;
            }
        }
    }

    /* Component variable changes */

    /*
     * Invoked when the value of a variable has changed. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    public void changeVariables(Object source, Map<String, Object> variables) {

    }

    /* General event framework */

    private static final Method COMPONENT_EVENT_METHOD = ReflectTools
            .findMethod(Component.Listener.class, "componentEvent",
                    Component.Event.class);

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * This method additionally informs the event-api to route events with the
     * given eventIdentifier to the components handleEvent function call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to listen for
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     * 
     * @since 6.2
     */
    protected void addListener(String eventIdentifier, Class<?> eventType,
            Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        if (eventIdentifiers == null) {
            eventIdentifiers = new HashSet<String>();
        }
        boolean needRepaint = !eventRouter.hasListeners(eventType);
        eventRouter.addListener(eventType, target, method);

        if (needRepaint) {
            eventIdentifiers.add(eventIdentifier);
            requestRepaint();
        }
    }

    /**
     * Checks if the given {@link Event} type is listened for this component.
     * 
     * @param eventType
     *            the event type to be checked
     * @return true if a listener is registered for the given event type
     */
    protected boolean hasListeners(Class<?> eventType) {
        return eventRouter != null && eventRouter.hasListeners(eventType);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * This method additionally informs the event-api to stop routing events
     * with the given eventIdentifier to the components handleEvent function
     * call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to stop listening for
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     * 
     * @since 6.2
     */
    protected void removeListener(String eventIdentifier, Class<?> eventType,
            Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
            if (!eventRouter.hasListeners(eventType)) {
                eventIdentifiers.remove(eventIdentifier);
                requestRepaint();
            }
        }
    }

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     */
    public void addListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, method);
    }

    /**
     * <p>
     * Convenience method for registering a new listener with the specified
     * activation method to listen events generated by this component. If the
     * activation method does not have any arguments the event object will not
     * be passed to it when it's called.
     * </p>
     * 
     * <p>
     * This version of <code>addListener</code> gets the name of the activation
     * method as a parameter. The actual method is reflected from
     * <code>object</code>, and unless exactly one match is found,
     * <code>java.lang.IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * <p>
     * Note: Using this method is discouraged because it cannot be checked
     * during compilation. Use {@link #addListener(Class, Object, Method)} or
     * {@link #addListener(com.vaadin.ui.Component.Listener)} instead.
     * </p>
     * 
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param methodName
     *            the name of the activation method.
     */
    public void addListener(Class<?> eventType, Object target, String methodName) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, methodName);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     */
    public void removeListener(Class<?> eventType, Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /**
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            target object that has registered to listen to events of type
     *            <code>eventType</code> with one or more methods.
     * @param method
     *            the method owned by <code>target</code> that's registered to
     *            listen to events of type <code>eventType</code>.
     */
    public void removeListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /**
     * <p>
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     * </p>
     * 
     * <p>
     * This version of <code>removeListener</code> gets the name of the
     * activation method as a parameter. The actual method is reflected from
     * <code>target</code>, and unless exactly one match is found,
     * <code>java.lang.IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     * @param methodName
     *            the name of the method owned by <code>target</code> that's
     *            registered to listen to events of type <code>eventType</code>.
     */
    public void removeListener(Class<?> eventType, Object target,
            String methodName) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, methodName);
        }
    }

    /**
     * Returns all listeners that are registered for the given event type or one
     * of its subclasses.
     * 
     * @param eventType
     *            The type of event to return listeners for.
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    public Collection<?> getListeners(Class<?> eventType) {
        if (eventType.isAssignableFrom(RepaintRequestEvent.class)) {
            // RepaintRequestListeners are not stored in eventRouter
            if (repaintRequestListeners == null) {
                return Collections.EMPTY_LIST;
            } else {
                return Collections
                        .unmodifiableCollection(repaintRequestListeners);
            }
        }
        if (eventRouter == null) {
            return Collections.EMPTY_LIST;
        }

        return eventRouter.getListeners(eventType);
    }

    /**
     * Sends the event to all listeners.
     * 
     * @param event
     *            the Event to be sent to all listeners.
     */
    protected void fireEvent(Component.Event event) {
        if (eventRouter != null) {
            eventRouter.fireEvent(event);
        }

    }

    /* Component event framework */

    /*
     * Registers a new listener to listen events generated by this component.
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    public void addListener(Component.Listener listener) {
        addListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /*
     * Removes a previously registered listener from this component. Don't add a
     * JavaDoc comment here, we use the default documentation from implemented
     * interface.
     */
    public void removeListener(Component.Listener listener) {
        removeListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /**
     * Emits the component event. It is transmitted to all registered listeners
     * interested in such events.
     */
    protected void fireComponentEvent() {
        fireEvent(new Component.Event(this));
    }

    /**
     * Emits the component error event. It is transmitted to all registered
     * listeners interested in such events.
     */
    protected void fireComponentErrorEvent() {
        fireEvent(new Component.ErrorEvent(getComponentError(), this));
    }

    /**
     * Sets the data object, that can be used for any application specific data.
     * The component does not use or modify this data.
     * 
     * @param data
     *            the Application specific data.
     * @since 3.1
     */
    public void setData(Object data) {
        applicationData = data;
    }

    /**
     * Gets the application specific data. See {@link #setData(Object)}.
     * 
     * @return the Application specific data set with setData function.
     * @since 3.1
     */
    public Object getData() {
        return applicationData;
    }

    /* Sizeable and other size related methods */

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#getHeight()
     */
    public float getHeight() {
        return height;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#getHeightUnits()
     */
    public Unit getHeightUnits() {
        return heightUnit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#getWidth()
     */
    public float getWidth() {
        return width;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#getWidthUnits()
     */
    public Unit getWidthUnits() {
        return widthUnit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setHeight(float, Unit)
     */
    public void setHeight(float height, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.height = height;
        heightUnit = unit;
        requestRepaint();
        // ComponentSizeValidator.setHeightLocation(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setSizeFull()
     */
    public void setSizeFull() {
        setWidth(100, Unit.PERCENTAGE);
        setHeight(100, Unit.PERCENTAGE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setSizeUndefined()
     */
    public void setSizeUndefined() {
        setWidth(-1, Unit.PIXELS);
        setHeight(-1, Unit.PIXELS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setWidth(float, Unit)
     */
    public void setWidth(float width, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.width = width;
        widthUnit = unit;
        requestRepaint();
        // ComponentSizeValidator.setWidthLocation(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setWidth(java.lang.String)
     */
    public void setWidth(String width) {
        Size size = parseStringSize(width);
        if (size != null) {
            setWidth(size.getSize(), size.getUnit());
        } else {
            setWidth(-1, Unit.PIXELS);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.Sizeable#setHeight(java.lang.String)
     */
    public void setHeight(String height) {
        Size size = parseStringSize(height);
        if (size != null) {
            setHeight(size.getSize(), size.getUnit());
        } else {
            setHeight(-1, Unit.PIXELS);
        }
    }

    /*
     * Returns array with size in index 0 unit in index 1. Null or empty string
     * will produce {-1,Unit#PIXELS}
     */
    private static Size parseStringSize(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if ("".equals(s)) {
            return null;
        }
        float size = 0;
        Unit unit = null;
        Matcher matcher = sizePattern.matcher(s);
        if (matcher.find()) {
            size = Float.parseFloat(matcher.group(1));
            if (size < 0) {
                size = -1;
                unit = Unit.PIXELS;
            } else {
                String symbol = matcher.group(3);
                unit = Unit.getUnitFromSymbol(symbol);
            }
        } else {
            throw new IllegalArgumentException("Invalid size argument: \"" + s
                    + "\" (should match " + sizePattern.pattern() + ")");
        }
        return new Size(size, unit);
    }

    private static class Size implements Serializable {
        float size;
        Unit unit;

        public Size(float size, Unit unit) {
            this.size = size;
            this.unit = unit;
        }

        public float getSize() {
            return size;
        }

        public Unit getUnit() {
            return unit;
        }
    }

    public interface ComponentErrorEvent extends Terminal.ErrorEvent {
    }

    public interface ComponentErrorHandler extends Serializable {
        /**
         * Handle the component error
         * 
         * @param event
         * @return True if the error has been handled False, otherwise
         */
        public boolean handleComponentError(ComponentErrorEvent event);
    }

    /**
     * Gets the error handler for the component.
     * 
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client.
     * 
     * @return
     */
    public ComponentErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler for the component.
     * 
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client.
     * 
     * If the error handler is not set, the application error handler is used to
     * handle the exception.
     * 
     * @param errorHandler
     *            AbstractField specific error handler
     */
    public void setErrorHandler(ComponentErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Handle the component error event.
     * 
     * @param error
     *            Error event to handle
     * @return True if the error has been handled False, otherwise. If the error
     *         haven't been handled by this component, it will be handled in the
     *         application error handler.
     */
    public boolean handleError(ComponentErrorEvent error) {
        if (errorHandler != null) {
            return errorHandler.handleComponentError(error);
        }
        return false;

    }

    /*
     * Actions
     */

    /**
     * Gets the {@link ActionManager} used to manage the
     * {@link ShortcutListener}s added to this {@link Field}.
     * 
     * @return the ActionManager in use
     */
    protected ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = new ActionManager();
            if (getRoot() != null) {
                actionManager.setViewer(getRoot());
            }
        }
        return actionManager;
    }

    public void addShortcutListener(ShortcutListener shortcut) {
        getActionManager().addAction(shortcut);
    }

    public void removeShortcutListener(ShortcutListener shortcut) {
        if (actionManager != null) {
            actionManager.removeAction(shortcut);
        }
    }

    /**
     * Registers an RPC interface implementation for this component.
     * 
     * A component can listen to multiple RPC interfaces, and subclasses can
     * register additional implementations.
     * 
     * @since 7.0
     * 
     * @param implementation
     *            RPC interface implementation
     * @param rpcInterfaceType
     *            RPC interface class for which the implementation should be
     *            registered
     */
    protected <T> void registerRpcImplementation(T implementation,
            Class<T> rpcInterfaceType) {
        if (this instanceof RpcTarget) {
            rpcManagerMap.put(rpcInterfaceType, new ServerRpcManager<T>(
                    (RpcTarget) this, implementation, rpcInterfaceType));
        } else {
            throw new RuntimeException(
                    "Cannot register an RPC implementation for a component that is not an RpcTarget");
        }
    }

    public RpcManager getRpcManager(Class<?> rpcInterface) {
        return rpcManagerMap.get(rpcInterface);
    }

}
