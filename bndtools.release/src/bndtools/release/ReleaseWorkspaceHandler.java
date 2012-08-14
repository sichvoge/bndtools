//package bndtools.release;
//
//import org.eclipse.core.commands.AbstractHandler;
//import org.eclipse.core.commands.ExecutionEvent;
//import org.eclipse.core.commands.ExecutionException;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.ui.PlatformUI;
//
//public class ReleaseWorkspaceHandler extends AbstractHandler {
//
//	public Object execute(ExecutionEvent event) throws ExecutionException {
//		try {
//
//			if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
//				return null;
//			}
//
//			WorkspaceAnalyserJob job = new WorkspaceAnalyserJob();
//			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
//			job.schedule();
//
//		} catch (Exception e) {
//			throw new ExecutionException(e.getMessage(), e);
//		}
//
//		return null;
//	}
//}
