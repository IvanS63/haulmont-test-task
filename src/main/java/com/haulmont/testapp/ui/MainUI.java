package com.haulmont.testapp.ui;


import com.haulmont.testapp.entity.Group;
import com.haulmont.testapp.entity.Student;
import com.haulmont.testapp.exception.GroupDeleteException;
import com.haulmont.testapp.service.IGroupService;
import com.haulmont.testapp.service.IStudentService;
import com.haulmont.testapp.service.impl.GroupService;
import com.haulmont.testapp.service.impl.StudentService;
import com.haulmont.testapp.ui.window.AbstractEditWindow;
import com.haulmont.testapp.ui.window.GroupEditWindow;
import com.haulmont.testapp.ui.window.StudentEditWindow;
import com.vaadin.annotations.Theme;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;
import org.apache.log4j.Logger;

@Theme("mytheme")
public class MainUI extends UI {
    private final static Logger logger = Logger.getLogger(MainUI.class);

    private IStudentService studentService;

    private IGroupService groupService;

    private Grid studentsGrid = new Grid();
    private Grid groupsGrid = new Grid();

    private Student student;
    private Group group;


    private final Button openAddGroupModalWindowBtn = new Button("Add group");
    private final Button openAddStudentModalWindowBtn = new Button("Add student");
    private final Button openEditStudentModalWindowBtn = new Button("Edit student");
    private final Button openEditGroupModalWindowBtn = new Button("Edit Group");
    private final Button deleteStudentBtn = new Button("Delete student");
    private final Button deleteGroupBtn = new Button("Delete group");

    private final TextField filterText = new TextField();
    private final Label filterLabel = new Label("Select filter: ");
    private final NativeSelect filterOption = new NativeSelect();
    private final String[] filterOptionValues = new String[]{"Lastname", "Group"};
    private final VerticalLayout layout = new VerticalLayout();


    @Override
    protected void init(VaadinRequest vaadinRequest) {
        studentService = StudentService.getInstance();
        groupService = GroupService.getInstance();
        initFormComponents();
        initListeners();
    }

    /**
     * Init form components and layout
     */
    private void initFormComponents() {
        groupsGrid.setColumns("number", "faculty");
        studentsGrid.setColumns("firstName", "middleName", "lastName", "birthDate", "group");
        TabSheet tabsheet = new TabSheet();
        layout.addComponent(tabsheet);

        VerticalLayout studentTabVerticalLayout = new VerticalLayout();
        VerticalLayout groupTabVerticalLayout = new VerticalLayout();
        HorizontalLayout filterElementsLayout = new HorizontalLayout(filterLabel, filterOption, filterText);
        filterElementsLayout.setMargin(true);
        filterElementsLayout.setSpacing(true);

        HorizontalLayout studentBtnLayout = new HorizontalLayout(
                openAddStudentModalWindowBtn, openEditStudentModalWindowBtn, deleteStudentBtn);
        studentBtnLayout.setMargin(true);
        studentBtnLayout.setSpacing(true);
        HorizontalLayout groupBtnLayout = new HorizontalLayout(
                openAddGroupModalWindowBtn, openEditGroupModalWindowBtn, deleteGroupBtn);
        groupBtnLayout.setSpacing(true);
        groupBtnLayout.setMargin(true);


        studentTabVerticalLayout.addComponent(filterElementsLayout);
        studentTabVerticalLayout.addComponent(studentsGrid);
        studentTabVerticalLayout.addComponent(studentBtnLayout);
        groupTabVerticalLayout.addComponent(groupsGrid);
        groupTabVerticalLayout.addComponent(groupBtnLayout);

        tabsheet.addTab(studentTabVerticalLayout, "Students");
        tabsheet.addTab(groupTabVerticalLayout, "Groups");

        layout.setComponentAlignment(tabsheet, Alignment.TOP_CENTER);
        tabsheet.setSizeUndefined();
        layout.setMargin(true);
        setContent(layout);

        updateStudentTable();
        updateGroupTable();

        openEditGroupModalWindowBtn.setEnabled(false);
        deleteGroupBtn.setEnabled(false);
        openEditStudentModalWindowBtn.setEnabled(false);
        deleteStudentBtn.setEnabled(false);

        filterText.setInputPrompt("Type here...");
        initSelectBox();

    }

    /**
     * Set filter options to select box
     */
    private void initSelectBox() {
        for (String s : filterOptionValues) {
            filterOption.addItem(s);
        }
        filterOption.setNullSelectionAllowed(false);
        filterOption.setValue(filterOptionValues[0]);
    }

    /**
     * Initialize button and selected table rows listeners
     */
    private void initListeners() {
        openAddGroupModalWindowBtn.addClickListener(e -> openGroupModal(null));
        openAddStudentModalWindowBtn.addClickListener(e -> openStudentModal(null));
        openEditGroupModalWindowBtn.addClickListener(e -> openGroupModal(group));
        openEditStudentModalWindowBtn.addClickListener(e -> openStudentModal(student));
        deleteGroupBtn.addClickListener(e -> {
            deleteGroup(group);
            updateGroupTable();
        });
        deleteStudentBtn.addClickListener(e -> deleteStudent(student));

        studentsGrid.addSelectionListener(e -> {
            student = (Student) studentsGrid.getSelectedRow();
            deleteStudentBtn.setEnabled(true);
            openEditStudentModalWindowBtn.setEnabled(true);
        });

        groupsGrid.addSelectionListener(e -> {
            group = (Group) groupsGrid.getSelectedRow();
            deleteGroupBtn.setEnabled(true);
            openEditGroupModalWindowBtn.setEnabled(true);
        });

        filterOption.addValueChangeListener(e -> {
            if (filterOption.getValue().equals("Group")) {
                filterText.addTextChangeListener(ev -> studentsGrid.setContainerDataSource(new BeanItemContainer<>(Student.class,
                        studentService.getAllFilteredByGroup(ev.getText()))));
            } else {
                filterText.addTextChangeListener(ex ->
                        studentsGrid.setContainerDataSource(new BeanItemContainer<>(Student.class,
                                studentService.getAllFilteredByLastname(ex.getText()))));
            }
        });

    }

    /**
     * Open group modal window
     *
     * @param group if null then to be added, otherwise to be edited
     */
    private void openGroupModal(Group group) {
        AbstractEditWindow editWindow = new GroupEditWindow(group);
        addWindow(editWindow);
        editWindow.addCloseListener(e -> updateGroupTable());
    }

    /**
     * Open student modal window
     *
     * @param student if null then to be added, otherwise to be edited
     */
    private void openStudentModal(Student student) {
        AbstractEditWindow editWindow = new StudentEditWindow(student);
        addWindow(editWindow);
        editWindow.addCloseListener(e -> updateStudentTable());
    }

    /**
     * Delete selected student
     *
     * @param student to be deleted
     */
    private void deleteStudent(Student student) {
        studentService.remove(student);
        updateStudentTable();
    }

    /**
     * Delete selected group
     *
     * @param group to be deleted
     */
    private void deleteGroup(Group group) {
        try {
            groupService.remove(group);
            updateGroupTable();
        } catch (GroupDeleteException ex) {
            updateGroupTable();
            Notification.show("This group contains students! You can't delete it!", Notification.Type.ERROR_MESSAGE);
        }
    }

    /**
     * Refresh student table from database
     */
    private void updateStudentTable() {
        studentsGrid.setContainerDataSource(new BeanItemContainer<>(Student.class, studentService.getAll()));
        setEditDeleteBtnsDisabled();
    }

    /**
     * Refresh group table from database
     */
    private void updateGroupTable() {
        groupsGrid.setContainerDataSource(new BeanItemContainer<>(Group.class, groupService.getAll()));
        setEditDeleteBtnsDisabled();
    }

    /**
     * Set EDIT/DELETE buttons disabled in order not to edit/delete NULL entity
     */
    private void setEditDeleteBtnsDisabled() {
        deleteGroupBtn.setEnabled(false);
        openEditGroupModalWindowBtn.setEnabled(false);
        deleteStudentBtn.setEnabled(false);
        openEditStudentModalWindowBtn.setEnabled(false);
    }


}
