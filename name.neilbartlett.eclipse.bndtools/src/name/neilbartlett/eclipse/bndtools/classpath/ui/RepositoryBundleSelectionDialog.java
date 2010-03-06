/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath.ui;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.repos.IBundleLocation;
import name.neilbartlett.eclipse.bndtools.repos.workspace.WorkspaceRepository;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class RepositoryBundleSelectionDialog extends TitleAreaDialog {

	private TableViewer viewer;
	private ISelection selection;

	public RepositoryBundleSelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		final Text txtFilter = new Text(composite, SWT.BORDER | SWT.SEARCH);
		Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Symbolic-Name; Version");
		col.setWidth(250);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Location");
		col.setWidth(250);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new BundleLocationLabelProvider());
		viewer.setInput(WorkspaceRepository.getInstance().getAllBundles());
		
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IBundleLocation loc1 = (IBundleLocation) e1;
				IBundleLocation loc2 = (IBundleLocation) e2;
				
				return loc1.compareTo(loc2);
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selection = viewer.getSelection();
				updateButtons();
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				selection = viewer.getSelection();
				
				setReturnCode(OK);
				close();
			}
		});
		
		txtFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				final String filterStr = txtFilter.getText().trim().toLowerCase();
				if(filterStr.length() > 0) {
					ViewerFilter filter = new ViewerFilter() {
						public boolean select(Viewer viewer, Object parentElement, Object element) {
							IBundleLocation location = (IBundleLocation) element;
							return location.getSymbolicName().toLowerCase().indexOf(filterStr) > -1;
						}
					};
					viewer.setFilters(new ViewerFilter[] { filter });
				} else {
					viewer.setFilters(new ViewerFilter[0]);
				}
			}
		});
		
		// Layout
		GridLayout layout = (GridLayout) composite.getLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}
	boolean validate() {
		return selection != null && !selection.isEmpty();
	}
	void updateButtons() {
		boolean valid = validate();
		Button okButton = getButton(IDialogConstants.OK_ID);
		if(okButton != null) {
			okButton.setEnabled(valid);
		}
	}
	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if(id == IDialogConstants.OK_ID) {
			button.setEnabled(validate());
		}
		return button;
	}
	public ISelection getSelection() {
		return selection;
	}
}
class BundleLocationLabelProvider extends StyledCellLabelProvider {
	
	Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
	
	@Override
	public void update(ViewerCell cell) {
		IBundleLocation location = (IBundleLocation) cell.getElement();
		StyledString styledString = null;
		if(cell.getColumnIndex() == 0) {
			styledString = getStyledBundleId(location);
			//cell.setImage(bundleImg);
		} else if(cell.getColumnIndex() == 1) {
			styledString = getStyledLocation(location);
		}
		
		if(styledString != null) {
			cell.setText(styledString.getString());
			cell.setStyleRanges(styledString.getStyleRanges());
		}
	}
	private StyledString getStyledBundleId(IBundleLocation location) {
		StyledString styledString = new StyledString(location.getSymbolicName());
		styledString.append("; " + location.getVersion(), StyledString.COUNTER_STYLER);
		return styledString;
	}
	private StyledString getStyledLocation(IBundleLocation location) {
		return new StyledString(location.getPath().makeRelative().toString(), StyledString.DECORATIONS_STYLER);
	}
	@Override
	public void dispose() {
		super.dispose();
		bundleImg.dispose();
	}
}
