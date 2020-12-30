package tech.cdf.remotedebug.model;

public class RunTarget {
	public String JarFile;
	public String ClassName;
	public String Args;
	public String Data;
	public RunMode Runmode;

	public RunTarget(String jarfile, String classname, String args, String data, RunMode rm) {
		this.JarFile = jarfile;
		this.ClassName = classname;
		this.Args = args;
		this.Data = data;
		this.Runmode = rm;
	}

	public static enum RunMode {
		RUN(0), DEBUG(1);
		private int index;

		private RunMode(int arg0) {
			this.setIndex(arg0);
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int i) {
			this.index = i;
		}
	}
}