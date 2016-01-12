/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.bsm.vaadin.adminpage;

import java.util.Objects;

import org.opennms.netmgt.bsm.service.BusinessServiceManager;
import org.opennms.netmgt.bsm.service.model.BusinessServiceDTO;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.vaadin.core.UIHelper;

import com.google.common.base.Strings;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * This class represents the main  Vaadin component for editing Business Service definitions.
 *
 * @author Markus Neumann <markus@opennms.com>
 * @author Christian Pape <christian@opennms.org>
 */
public class BusinessServiceMainLayout extends VerticalLayout {
    /**
     * the Business Service Manager instance
     */
    private final BusinessServiceManager m_businessServiceManager;

    private final EventForwarder m_eventForwarder;

    /**
     * the table instance
     */
    private final Table m_table;

    /**
     * the bean item container for the listed Business Service DTOs
     */
    private final BeanItemContainer<BusinessServiceDTO> m_beanItemContainer = new BeanItemContainer<>(BusinessServiceDTO.class);

    public BusinessServiceMainLayout(BusinessServiceManager businessServiceManager, EventForwarder eventForwarder) {
        m_businessServiceManager = Objects.requireNonNull(businessServiceManager);
        m_eventForwarder = Objects.requireNonNull(eventForwarder);

        setSizeFull();

        // construct the upper layout for the create button and field
        HorizontalLayout upperLayout = new HorizontalLayout();

        // Reload button to allow manual reloads of the state machine
        final Button reloadButton = UIHelper.createButton("Reload", "Reloads the Business Service State Machine", null, (Button.ClickListener) event -> {
            EventBuilder eventBuilder = new EventBuilder(EventConstants.RELOAD_DAEMON_CONFIG_UEI, "BSM Master Page");
            eventBuilder.addParam(EventConstants.PARM_DAEMON_NAME, "bsmd");
            m_eventForwarder.sendNow(eventBuilder.getEvent());
        });

        // business service input field
        final TextField createTextField = new TextField();
        createTextField.setInputPrompt("Business Service Name");
        createTextField.setId("createTextField");

        // create button
        final Button createButton = new Button("Create");
        createButton.setId("createButton");
        createButton.addClickListener((Button.ClickListener) event -> {
            /**
             * check for valid value
             */
            if (!"".equals(Strings.nullToEmpty(createTextField.getValue()).trim())) {
                /**
                 * create new DTO instance
                 */
                final BusinessServiceDTO businessServiceDTO = new BusinessServiceDTO();
                /**
                 * add the title
                 */
                businessServiceDTO.setName(createTextField.getValue().trim());
                /**
                 * create the modal configuration dialog
                 */
                getUI().addWindow(new BusinessServiceEditWindow(businessServiceDTO, BusinessServiceMainLayout.this));
                /**
                 * clear the textfield value
                 */
                createTextField.setValue("");
            }
        });

        /**
         * add to the upper layout
         */
        upperLayout.addComponent(reloadButton);
        upperLayout.addComponent(createTextField);
        upperLayout.addComponent(createButton);
        addComponent(upperLayout);
        /**
         * and set the upper-right alignment
         */
        setComponentAlignment(upperLayout, Alignment.TOP_RIGHT);

        /**
         * now construct the table...
         */
        m_table = new Table();
        m_table.setSizeFull();
        m_table.setContainerDataSource(m_beanItemContainer);

        /**
         * ...and configure the visible columns
         */
        m_table.setVisibleColumns("id", "name");

        /**
         * create generated columns for modification of entries...
         */
        m_table.addGeneratedColumn("edit", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                Button editButton = new Button("edit");
                editButton.setId("editButton-" + ((BusinessServiceDTO) itemId).getName());

                editButton.addClickListener((Button.ClickListener) event -> {
                    getUI().addWindow(new BusinessServiceEditWindow((BusinessServiceDTO) itemId, BusinessServiceMainLayout.this));
                    refreshTable();
                });
                return editButton;
            }
        });

        /**
         * ...and deletion of entries
         */
        m_table.addGeneratedColumn("delete", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                Button deleteButton = new Button("delete");
                deleteButton.setId("deleteButton-" + ((BusinessServiceDTO) itemId).getName());
                deleteButton.addClickListener((Button.ClickListener) event -> {
                    businessServiceManager.delete(((BusinessServiceDTO) itemId).getId());
                    refreshTable();
                });
                return deleteButton;
            }
        });

        /**
         * add the table to the layout
         */
        addComponent(m_table);
        setExpandRatio(m_table, 1.0f);

        /**
         * initial refresh of table
         */
        refreshTable();
    }

    /**
     * Returns the Business Service Manager instance associated with this instance.
     *
     * @return the instance of the associated Business Service Manager
     */
    public BusinessServiceManager getBusinessServiceManager() {
        return m_businessServiceManager;
    }

    /**
     * Refreshes the entries of the table used for listing the DTO instances.
     */
    public void refreshTable() {
        /**
         * remove all...
         */
        m_beanItemContainer.removeAllItems();
        /**
         * ...and add all DTOs found by the service instance.
         */
        m_beanItemContainer.addAll(m_businessServiceManager.findAll());
    }
}
