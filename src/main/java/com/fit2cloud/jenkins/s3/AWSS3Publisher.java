package com.fit2cloud.jenkins.s3;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class AWSS3Publisher extends Publisher {

	private PrintStream logger;
	String bucketName;
	String filesPath;
	String objectPrefix;
	
	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getFilesPath() {
		return filesPath;
	}

	public void setFilesPath(String filesPath) {
		this.filesPath = filesPath;
	}

	public String getObjectPrefix() {
		return objectPrefix;
	}

	public void setObjectPrefix(String objectPrefix) {
		this.objectPrefix = objectPrefix;
	}

	@DataBoundConstructor
	public AWSS3Publisher(final String bucketName, final String filesPath, final String objectPrefix) {
		this.bucketName = bucketName;
		this.filesPath = filesPath;
		this.objectPrefix = objectPrefix;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}

	@Override
	public DescriptorImpl getDescriptor() {

		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		private String awsAccessKey;
		private String awsSecretKey;

		public DescriptorImpl() {
			super(AWSS3Publisher.class);
			load();
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return super.newInstance(req, formData);
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "上传Artifacts到S3";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			req.bindParameters(this);
			this.awsAccessKey        = formData.getString("awsAccessKey");
			this.awsSecretKey        = formData.getString("awsSecretKey");
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckAccount(
				@QueryParameter String awsAccessKey,
				@QueryParameter String awsSecretKey,
				@QueryParameter String aliyunEndPointSuffix) {
			if (Utils.isNullOrEmpty(awsAccessKey)) {
				return FormValidation.error("AccessKey不能为空！");
			}
			if (Utils.isNullOrEmpty(awsSecretKey)) {
				return FormValidation.error("SecretKey不能为空！");
			}
			try {
				AWSS3Client.validateAWSAccount(awsAccessKey,
						awsSecretKey);
			} catch (Exception e) {
				return FormValidation.error(e.getMessage());
			}
			return FormValidation.ok("验证帐号成功！");
		}

		public FormValidation doCheckBucket(@QueryParameter String val)
				throws IOException, ServletException {
			if (Utils.isNullOrEmpty(val)) {
				return FormValidation.error("Bucket不能为空！");
			}
			try {
				AWSS3Client.validateS3Bucket(awsAccessKey,
						awsSecretKey, val);
			} catch (Exception e) {
				return FormValidation.error(e.getMessage());
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckPath(@QueryParameter String val) {
			if (Utils.isNullOrEmpty(val)) {
				return FormValidation.error("Artifact路径不能为空！");
			}
			return FormValidation.ok();
		}
		
		public String getawsAccessKey() {
			return awsAccessKey;
		}

		public void setawsAccessKey(String awsAccessKey) {
			this.awsAccessKey = awsAccessKey;
		}

		public String getawsSecretKey() {
			return awsSecretKey;
		}

		public void setawsSecretKey(String awsSecretKey) {
			this.awsSecretKey = awsSecretKey;
		}

	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,BuildListener listener) 
			throws InterruptedException, IOException {
		this.logger = listener.getLogger();
		final boolean buildFailed = build.getResult() == Result.FAILURE;
		if (buildFailed) {
			logger.println("Job构建失败,无需上传Aritfacts到S3.");
			return true;
		}
		
		// Resolve file path
		String expFP = Utils.replaceTokens(build, listener, filesPath);

		if (expFP != null) {
			expFP = expFP.trim();
		}

		// Resolve virtual path
		String expVP = Utils.replaceTokens(build, listener, objectPrefix);
		if (Utils.isNullOrEmpty(expVP)) {
			expVP = null;
		}
		if (!Utils.isNullOrEmpty(expVP) && !expVP.endsWith(Utils.FWD_SLASH)) {
			expVP = expVP.trim() + Utils.FWD_SLASH;
		}        
		
		boolean success = false;
		try {
			int filesUploaded = AWSS3Client.upload(build, listener,
                    this.getDescriptor().awsAccessKey,
					this.getDescriptor().awsSecretKey,
                    bucketName, expFP, expVP);
			if (filesUploaded > 0) { 
				listener.getLogger().println("上传Artifacts到S3成功，上传文件个数:" + filesUploaded);
				success = true;
			}

		} catch (Exception e) {
			this.logger.println("上传Artifact到S3失败，错误消息如下:");
			this.logger.println(e.getMessage());
			e.printStackTrace(this.logger);
			success = false;
		}
		return success;
	}

}
