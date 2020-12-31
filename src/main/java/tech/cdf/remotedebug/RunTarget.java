package tech.cdf.remotedebug;

public class RunTarget {
	public String JarFile;

	public String Args;

	public String Data;

	public RunMode Runmode;

	public RunTarget(String jarfile, String args, String data, RunMode rm) {
		this.JarFile = jarfile;
		this.Args = args;
		this.Data = data;
		this.Runmode = rm;
	}

	public enum RunMode {
		RUN(0), DEBUG(1), EXCEPTION(2);

		private int index;

		RunMode(int arg0) {
			setIndex(arg0);
		}

		public int getIndex() {
			return this.index;
		}

		public void setIndex(int i) {
			this.index = i;
		}
	}
}
