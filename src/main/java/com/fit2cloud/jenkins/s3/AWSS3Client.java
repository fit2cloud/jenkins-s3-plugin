package com.fit2cloud.jenkins.s3;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.apache.commons.lang.time.DurationFormatUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AWSS3Client {
	private static final String fpSeparator = ";";

	public static boolean validateAWSAccount(
			final String awsAccessKey, final String awsSecretKey) throws AWSS3Exception {
		try {
			AmazonEC2Client client = new AmazonEC2Client(new BasicAWSCredentials(awsAccessKey,awsSecretKey));
			try {
				client.describeRegions();
			} catch (Exception e) {
				client.setRegion(RegionUtils.getRegion("cn-north-1"));
				client.describeRegions();
			}
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			throw new AWSS3Exception("账号验证失败：" + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AWSS3Exception("账号验证失败：" + e.getMessage());
		}
        return true;
	}


	public static boolean validateS3Bucket(String awsAccessKey,
			String awsSecretKey, String bucketName) throws AWSS3Exception{
		try {
			AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
			client.setRegion(RegionUtils.getRegion("cn-north-1"));
			return client.doesBucketExist(bucketName);
		} catch (Exception e) {
			try {
				AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
				client.doesBucketExist(bucketName);
			} catch (Exception e1) {
				throw new AWSS3Exception("验证Bucket名称失败：" + e.getMessage());
			}
		}
		return true;
	}
	
	public static int upload(AbstractBuild<?, ?> build, BuildListener listener,
			final String awsAccessKey, final String awsSecretKey, String bucketName,String expFP,String expVP) throws AWSS3Exception {
		AmazonS3Client client = null;
		try {
			client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
			client.setRegion(RegionUtils.getRegion("cn-north-1"));
			client.doesBucketExist(bucketName);
		} catch (Exception e) {
			try {
				client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
				client.doesBucketExist(bucketName);
			} catch (Exception e1) {
				throw new AWSS3Exception("无效的AWS 账号。");
			}
		}
		
		int filesUploaded = 0; // Counter to track no. of files that are uploaded
		try {
			FilePath workspacePath = build.getWorkspace();
			if (workspacePath == null) {
				listener.getLogger().println("工作空间中没有任何文件.");
				return filesUploaded;
			}
			StringTokenizer strTokens = new StringTokenizer(expFP, fpSeparator);
			FilePath[] paths = null;

			listener.getLogger().println("开始上传到AWS S3...");

			while (strTokens.hasMoreElements()) {
				String fileName = strTokens.nextToken();
				String embeddedVP = null;
				if (fileName != null) {
					int embVPSepIndex = fileName.indexOf("::");
					if (embVPSepIndex != -1) {
						if (fileName.length() > embVPSepIndex + 1) {
							embeddedVP = fileName.substring(embVPSepIndex + 2, fileName.length());
							if (Utils.isNullOrEmpty(embeddedVP)) {
								embeddedVP = null;
							}
							if (embeddedVP != null	&& !embeddedVP.endsWith(Utils.FWD_SLASH)) {
								embeddedVP = embeddedVP + Utils.FWD_SLASH;
							}
						}
						fileName = fileName.substring(0, embVPSepIndex);
					}
				}

				if (Utils.isNullOrEmpty(fileName)) {
					return filesUploaded;
				}

				FilePath fp = new FilePath(workspacePath, fileName);

				if (fp.exists() && !fp.isDirectory()) {
					paths = new FilePath[1];
					paths[0] = fp;
				} else {
					paths = workspacePath.list(fileName);
				}

				if (paths.length != 0) {
					for (FilePath src : paths) {
						String key = "";
						if (Utils.isNullOrEmpty(expVP)
								&& Utils.isNullOrEmpty(embeddedVP)) {
							key = src.getName();
						} else {
							String prefix = expVP;
							if (!Utils.isNullOrEmpty(embeddedVP)) {
								if (Utils.isNullOrEmpty(expVP)) {
									prefix = embeddedVP;
								} else {
									prefix = expVP + embeddedVP;
								}
							}
							key = prefix + src.getName();
						}
						long startTime = System.currentTimeMillis();
						InputStream inputStream = src.read();
						try {
							 ObjectMetadata metadata = new ObjectMetadata();
							    metadata.setContentLength(src.length());
				            client.putObject(new PutObjectRequest(
				            		                 bucketName, key, inputStream, metadata));
						} finally {
							try {
								inputStream.close();
							} catch (IOException e) {
							}
						}
						long endTime = System.currentTimeMillis();
						listener.getLogger().println("Uploaded object ["+ key + "] in " + getTime(endTime - startTime));
						filesUploaded++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new AWSS3Exception(e.getMessage(), e.getCause());
		}
		return filesUploaded;
	}
	
	public static String getTime(long timeInMills) {
		return DurationFormatUtils.formatDuration(timeInMills, "HH:mm:ss.S") + " (HH:mm:ss.S)";
	}

}
