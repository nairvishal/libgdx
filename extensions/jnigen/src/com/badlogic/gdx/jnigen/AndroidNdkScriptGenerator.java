package com.badlogic.gdx.jnigen;

import java.util.ArrayList;

import com.badlogic.gdx.jnigen.BuildTarget.TargetOs;
import com.badlogic.gdx.jnigen.FileDescriptor.FileType;

public class AndroidNdkScriptGenerator {
	public void generate(BuildConfig config, BuildTarget target) {
		if(target.os != TargetOs.Android) throw new IllegalArgumentException("target os must be Android");
		
		// create all the directories for outputing object files, shared libs and natives jar as well as build scripts.
		if(!config.libsDir.exists()) {
			if(!config.libsDir.mkdirs()) throw new RuntimeException("Couldn't create directory for shared library files in '" + config.libsDir + "'");
		}
		if(!config.jniDir.exists()) {
			if(!config.jniDir.mkdirs()) throw new RuntimeException("Couldn't create native code directory '" + config.jniDir + "'");
		}
		
		ArrayList<FileDescriptor> files = new ArrayList<FileDescriptor>();
		String[] includes = new String[target.cIncludes.length + target.cppIncludes.length];
		int idx = 0;
		for(String include: target.cIncludes) includes[idx++] = include;
		for(String include: target.cppIncludes) includes[idx++] = include;
		String[] excludes = new String[target.cExcludes.length + target.cppExcludes.length];
		for(String exclude: target.cExcludes) excludes[idx++] = exclude;
		for(String exclude: target.cppExcludes) excludes[idx++] = exclude;
		gatherSourceFiles(config.jniDir, includes, excludes, files);
		
		// create androidManifest.xml if it doesn't exist.
		FileDescriptor manifest = config.jniDir.parent();
		if(manifest.path().equals("/") || manifest.path().equals("\\")) manifest = new FileDescriptor("AndroidManifest.xml");
		else manifest = config.jniDir.parent().child("AndroidManifest.xml");
		if(!manifest.exists()) {
			manifest.writeString("", false);
		}
		
		// create Application.mk file
		FileDescriptor application = config.jniDir.child("Application.mk");
		application.writeString(new FileDescriptor("com/badlogic/gdx/jnigen/resources/scripts/Application.mk.template", FileType.Classpath).readString(), false);

		// create Android.mk file
		String template = new FileDescriptor("com/badlogic/gdx/jnigen/resources/scripts/Android.mk.template", FileType.Classpath).readString();
		
		StringBuffer srcFiles = new StringBuffer();
		for(int i = 0; i < files.size(); i++) {
			if(i > 0) srcFiles.append("\t");
			srcFiles.append(files.get(i).path().replace('\\', '/').replace(config.jniDir.toString() + "/", ""));
			if(i <files.size() - 1) srcFiles.append("\\\n");
			else srcFiles.append("\n");
		}
		
		StringBuffer headerDirs = new StringBuffer();
		for(String headerDir: target.headerDirs) {
			headerDirs.append(headerDir);
			headerDirs.append(" ");
		}

		template = template.replace("%sharedLibName%", config.sharedLibName);
		template = template.replace("%headerDirs%", headerDirs);
		template = template.replace("%cFlags%", target.cFlags);
		template = template.replace("%cppFlags%", target.cppFlags);
		template = template.replace("%linkerFlags%", target.linkerFlags);
		template = template.replace("%srcFiles%", srcFiles);
		
		System.out.println(template);
		config.jniDir.child("Android.mk").writeString(template, false);
	}
	
	private void gatherSourceFiles(FileDescriptor file, String[] includes, String[] excludes, ArrayList<FileDescriptor> files) {
		String fileName = file.path().replace('\\', '/');
		if(file.isDirectory()) {
			if(match(fileName, excludes)) return;
			for(FileDescriptor child: file.list()) {
				gatherSourceFiles(child, includes, excludes, files);
			}
		} else {
			if(match(fileName, includes)) files.add(file);
		}
	}
	
	private boolean match(String file, String[] patterns) {
		AntPathMatcher matcher = new AntPathMatcher();
		for(String pattern: patterns) {
			if(matcher.match(pattern, file)) {
				return true;
			}
		}
		return false;
	}
}