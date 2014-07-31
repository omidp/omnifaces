/*
 * Copyright 2014 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.viewhandler;

import static javax.faces.component.UINamingContainer.getSeparatorChar;
import static javax.faces.component.UIViewRoot.UNIQUE_ID_PREFIX;
import static org.omnifaces.util.Components.findComponent;
import static org.omnifaces.util.Faces.getContext;

import java.io.IOException;
import java.io.Writer;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;

/**
 * <p>
 * This {@link ViewHandler} once installed will throw an {@link IllegalStateException} whenever an automatically
 * generated JSF component ID  (<code>j_id...</code>) is encountered in the rendered output.
 * This has various advantages:
 * <ul>
 * <li>Keep the HTML output free of autogenerated JSF component IDs.
 * <li>No need to fix the IDs again and again when the client side unit tester encounters an unusable autogenerated ID.
 * <li>Make the developer aware which components are naming containers and/or implicitly require outputting its ID.
 * </ul>
 * <p>
 * Note that this does not check every component for its ID directly, but instead checks the {@link ResponseWriter} for
 * writes to the "id" attribute. Components that write their markup in any other way won't be checked and will thus
 * slip through.
 *
 * <h3>Installation</h3>
 * <p>
 * Register it as <code>&lt;view-handler&gt;</code> in <code>faces-config.xml</code>.
 * <pre>
 * &lt;application&gt;
 *     &lt;view-handler&gt;org.omnifaces.viewhandler.NoAutoGeneratedIdViewHandler&lt;/view-handler&gt;
 * &lt;/application&gt;
 * </pre>
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class NoAutoGeneratedIdViewHandler extends ViewHandlerWrapper {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_AUTO_GENERATED_ID_ENCOUNTERED =
		"Auto generated ID '%s' encountered on component type: '%s'.";

	// Properties -----------------------------------------------------------------------------------------------------

	private ViewHandler wrapped;


	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new No Auto Generated Id view handler around the given wrapped view handler.
	 *
	 * @param wrapped
	 *            The wrapped view handler.
	 */
	public NoAutoGeneratedIdViewHandler(ViewHandler wrapped) {
		this.wrapped = wrapped;
	}


	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void renderView(final FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {

		super.renderView(new FacesContextWrapper() {

			@Override
			public void setResponseWriter(ResponseWriter responseWriter) {
				super.setResponseWriter(new NoAutoGeneratedIdResponseWriter(responseWriter));
			}

			@Override
			public FacesContext getWrapped() {
				return context;
			}

		}, viewToRender);
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This response writer throws an {@link IllegalStateException} when an attribute with name "id" is written with
	 * a non-null value which starts with {@link UIViewRoot#UNIQUE_ID_PREFIX} or contains an intermediate.
	 *
	 * @since 2.0
	 * @author Arjan Tijms
	 */
	public static class NoAutoGeneratedIdResponseWriter extends ResponseWriterWrapper {

		private ResponseWriter wrapped;
		private final char SEPARATOR_CHAR;
		private final String INTERMEDIATE_ID_PREFIX;

		public NoAutoGeneratedIdResponseWriter(ResponseWriter wrapped) {
			this.wrapped = wrapped;
			SEPARATOR_CHAR = getSeparatorChar(getContext());
			INTERMEDIATE_ID_PREFIX = SEPARATOR_CHAR + UNIQUE_ID_PREFIX;
		}

		@Override
		public ResponseWriter cloneWithWriter(Writer writer) {
			return new NoAutoGeneratedIdResponseWriter(super.cloneWithWriter(writer));
		}

		@Override
		public void writeAttribute(String name, Object value, String property) throws IOException {

			if (value != null && "id".equals(name)) {
				String id = value.toString();

				if (id.startsWith(UNIQUE_ID_PREFIX) || id.contains(INTERMEDIATE_ID_PREFIX)) {
					int end = id.indexOf(SEPARATOR_CHAR, id.indexOf(UNIQUE_ID_PREFIX));

					if (end > 0) {
						id = id.substring(0, end);
					}

					UIComponent component = findComponent(id);

					throw new IllegalStateException(String.format(ERROR_AUTO_GENERATED_ID_ENCOUNTERED,
						id, (component == null) ? "<null>" : component.getClass().getName()
					));
				}
			}

			super.writeAttribute(name, value, property);
		}

		@Override
		public ResponseWriter getWrapped() {
			return wrapped;
		}

	}

}
