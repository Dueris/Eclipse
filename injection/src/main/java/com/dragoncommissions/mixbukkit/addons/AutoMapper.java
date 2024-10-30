package com.dragoncommissions.mixbukkit.addons;

import com.dragoncommissions.mixbukkit.MixBukkit;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoMapper {

	private static final Logger log = LogManager.getLogger("AutoMapper");
	private static boolean prepared = false;
	private static File mappingFile;

	@SneakyThrows
	public static InputStream getMappingAsStream() {
		if (!prepared) {
			try {
				prepareMapping();
			} catch (Exception e) {
				if (MixBukkit.DEBUG) {
					e.printStackTrace();
				}
				log.error("[!] Error loading mapping! Have you connected to the internet?");
				if (MixBukkit.SAFE_MODE) {
					log.error("[!] Server shutdown because safe mode is on, not loading mapping correctly may cause critical bugs/saves corruption.");
					Bukkit.getServer().shutdown();
					throw e;
				}
			}
			prepared = true;
		}
		if (mappingFile == null) return null; // Don't load any mapping
		try {
			return new FileInputStream(mappingFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@SneakyThrows
	private static void prepareMapping() {
		if (!shouldLoadMapping()) {
			log.info("[!] You don't need any mapping for this build!");
			return;
		}
		mappingFile = new File("mappings.csrg");
		if (mappingFile.exists()) {
			if (!mappingFile.isDirectory()) {
				log.info("[!] Pre-downloaded mapping detected! Using it. If anything went wrong, please try deleting {}{}{} and try again", ChatColor.DARK_GRAY, mappingFile.getAbsolutePath(), ChatColor.YELLOW);
				return;
			}
			mappingFile.delete();
		}
		File buildDataDir = new File(Paths.get("cache/mixins/").toFile(), "BuildData");
		log.info("[!] Fetching BuildData version from Spigot API...");
		Gson gson = new Gson();
		URLConnection connection = null;
		try {
			connection = new URL("https://hub.spigotmc.org/versions/" + getMCVersion() + ".json").openConnection();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		JsonObject object = null;
		try {
			object = gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String buildDataVersion = object.get("refs").getAsJsonObject().get("BuildData").getAsString();
		Git buildData = null;
		log.info("[!] Fetched BuildData Version: {}!", buildDataVersion);
		if (buildDataDir.exists()) {
			log.info("[!] Found Spigot's BuildData cache at {}! Doing some simple verification...", buildDataDir.getAbsolutePath());
			try {
				buildData = Git.open(buildDataDir);
				log.info("[!] Verified! Updating BuildData...");
				buildData.pull().call();
			} catch (Exception e) {
				buildDataDir.delete();
			}
		}
		if (!buildDataDir.exists()) {
			log.info("[!] Cloning Spigot's BuildData repository to {} . It should take a while (Usually around 35 MB), but it's a one time process (across every server)", buildDataDir.getAbsolutePath());
			try {
				buildData = Git.cloneRepository().setURI("https://hub.spigotmc.org/stash/scm/spigot/builddata.git").setDirectory(buildDataDir).call();
			} catch (GitAPIException e) {
				throw new RuntimeException(e);
			}
		}

		log.info("[!] Successfully fetched BuildData! Switching to {}", buildDataVersion);
		try {
			buildData.checkout().setName(buildDataVersion).call();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
		log.info("[!] Checking version info...");
		VersionInfo versionInfo = null;
		try {
			versionInfo = gson.fromJson(new FileReader(new File(buildDataDir, "info.json")), VersionInfo.class);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		log.info("[!] Scanning for members mapping...");
		File classMappings = new File(buildDataDir, "mappings/" + versionInfo.classMappings);
		if (versionInfo.memberMappings == null) {
			log.info("[!] Didn't find a members mapping! Building one...");
			MapUtil mapUtil = new MapUtil();
			try {
				mapUtil.loadBuk(classMappings);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			log.info("[!] Downloading Minecraft's Mappings & Building Members Mappings...");
			InputStream inputStream = null;
			try {
				inputStream = new URL(versionInfo.mappingsUrl).openConnection().getInputStream();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			while (true) {
				int read = 0;
				try {
					read = inputStream.read();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if (read == -1) break;
				outputStream.write(read);
			}
			try {
				mapUtil.makeFieldMaps(outputStream.toString(), mappingFile, true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			log.info("[!] Found a pre-built members mapping! Extracting...");
			try {
				mappingFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try {
				Files.copy(new File(buildDataDir, "mappings/" + versionInfo.memberMappings).toPath(), mappingFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		log.info("[!] Finished loading mappings!");

	}

	public static String getMCVersion() {
		return "1.21.1";
	}

	private static boolean shouldLoadMapping() {
		return Integer.parseInt(getMCVersion().split("\\.")[1]) >= 17 && isObfuscatedBuild();
		// Remapped option is only available after 1.17
	}

	public static boolean isObfuscatedBuild() {
		try {
			Class<?> aClass = Class.forName("net.minecraft.world.entity.EntityLiving");
			return true;
		} catch (Throwable ignored) {
		}
		return false;
	}

	/**
	 * Source: Spigot BuildTools
	 */
	@Data
	@AllArgsConstructor
	private static class VersionInfo {

		private static final Pattern URL_PATTERN = Pattern.compile("https://launcher.mojang.com/v1/objects/([0-9a-f]{40})/.*");
		private final String minecraftVersion;
		private final String accessTransforms;
		private final String classMappings;
		private final String memberMappings;
		private final String packageMappings;
		private final String minecraftHash;
		private final int toolsVersion = -1;
		private String classMapCommand;
		private String memberMapCommand;
		private String finalMapCommand;
		private String decompileCommand;
		private String serverUrl;
		private String mappingsUrl;
		private String spigotVersion;

		public VersionInfo(String minecraftVersion, String accessTransforms, String classMappings, String memberMappings, String packageMappings, String minecraftHash) {
			this.minecraftVersion = minecraftVersion;
			this.accessTransforms = accessTransforms;
			this.classMappings = classMappings;
			this.memberMappings = memberMappings;
			this.packageMappings = packageMappings;
			this.minecraftHash = minecraftHash;
		}

		public VersionInfo(String minecraftVersion, String accessTransforms, String classMappings, String memberMappings, String packageMappings, String minecraftHash, String decompileCommand) {
			this.minecraftVersion = minecraftVersion;
			this.accessTransforms = accessTransforms;
			this.classMappings = classMappings;
			this.memberMappings = memberMappings;
			this.packageMappings = packageMappings;
			this.minecraftHash = minecraftHash;
			this.decompileCommand = decompileCommand;
		}

		public static String hashFromUrl(String url) {
			if (url == null) {
				return null;
			}

			Matcher match = URL_PATTERN.matcher(url);
			return (match.find()) ? match.group(1) : null;
		}

		public String getShaServerHash() {
			return hashFromUrl(serverUrl);
		}

		public String getShaMappingsHash() {
			return hashFromUrl(mappingsUrl);
		}
	}

	private static class MapUtil {

		private static final Pattern MEMBER_PATTERN = Pattern.compile("(?:\\d+:\\d+:)?(.*?) (.*?) \\-> (.*)");
		private final BiMap<String, String> obf2Buk = HashBiMap.create();
		private final BiMap<String, String> moj2Obf = HashBiMap.create();
		//
		private final List<String> header = new ArrayList<>();

		public static String deobfClass(String obf, Map<String, String> classMaps) {
			String buk = classMaps.get(obf);
			if (buk == null) {
				StringBuilder inner = new StringBuilder();

				while (buk == null) {
					int idx = obf.lastIndexOf('$');
					if (idx == -1) {
						return null;
					}
					inner.insert(0, obf.substring(idx));
					obf = obf.substring(0, idx);

					buk = classMaps.get(obf);
				}

				buk += inner;
			}
			return buk;
		}

		public static String toObf(String desc, Map<String, String> map) {
			desc = desc.substring(1);
			StringBuilder out = new StringBuilder("(");
			if (desc.charAt(0) == ')') {
				desc = desc.substring(1);
				out.append(')');
			}
			while (desc.length() > 0) {
				desc = obfType(desc, map, out);
				if (desc.length() > 0 && desc.charAt(0) == ')') {
					desc = desc.substring(1);
					out.append(')');
				}
			}
			return out.toString();
		}

		public static String obfType(String desc, Map<String, String> map, StringBuilder out) {
			int size = 1;
			switch (desc.charAt(0)) {
				case 'B':
				case 'C':
				case 'D':
				case 'F':
				case 'I':
				case 'J':
				case 'S':
				case 'Z':
				case 'V':
					out.append(desc.charAt(0));
					break;
				case '[':
					out.append("[");
					return obfType(desc.substring(1), map, out);
				case 'L':
					String type = desc.substring(1, desc.indexOf(";"));
					size += type.length() + 1;
					out.append("L").append(map.containsKey(type) ? map.get(type) : type).append(";");
			}
			return desc.substring(size);
		}

		private static String csrgDesc(Map<String, String> first, Map<String, String> second, String args, String ret) {
			String[] parts = args.substring(1, args.length() - 1).split(",");
			StringBuilder desc = new StringBuilder("(");
			for (String part : parts) {
				if (part.isEmpty()) {
					continue;
				}
				desc.append(toJVMType(first, second, part));
			}
			desc.append(")");
			desc.append(toJVMType(first, second, ret));
			return desc.toString();
		}

		private static String toJVMType(Map<String, String> first, Map<String, String> second, String type) {
			switch (type) {
				case "byte":
					return "B";
				case "char":
					return "C";
				case "double":
					return "D";
				case "float":
					return "F";
				case "int":
					return "I";
				case "long":
					return "J";
				case "short":
					return "S";
				case "boolean":
					return "Z";
				case "void":
					return "V";
				default:
					if (type.endsWith("[]")) {
						return "[" + toJVMType(first, second, type.substring(0, type.length() - 2));
					}
					String clazzType = type.replace('.', '/');
					String obf = deobfClass(clazzType, first);
					String mappedType = deobfClass((obf != null) ? obf : clazzType, second);

					return "L" + ((mappedType != null) ? mappedType : clazzType) + ";";
			}
		}

		public void loadBuk(File bukClasses) throws IOException {
			for (String line : Files.readAllLines(bukClasses.toPath())) {
				if (line.startsWith("#")) {
					header.add(line);
					continue;
				}

				String[] split = line.split(" ");
				if (split.length == 2) {
					obf2Buk.put(split[0], split[1]);
				}
			}
		}

		public void makeFieldMaps(String mojIn, File fields, boolean includeMethods) throws IOException {
			List<String> lines = new ArrayList<>();
			if (includeMethods) {
				for (String line : mojIn.split("\n")) {
					lines.add(line);
					if (line.startsWith("#")) {
						continue;
					}

					if (line.endsWith(":")) {
						String[] parts = line.split(" -> ");
						String orig = parts[0].replace('.', '/');
						String obf = parts[1].substring(0, parts[1].length() - 1).replace('.', '/');

						moj2Obf.put(orig, obf);
					}
				}
			}

			List<String> outFields = new ArrayList<>(header);

			String currentClass = null;
			for (String line : mojIn.split("\n")) {
				if (line.startsWith("#")) {
					continue;
				}
				line = line.trim();

				if (line.endsWith(":")) {
					currentClass = null;

					String[] parts = line.split(" -> ");
					String orig = parts[0].replace('.', '/');
					String obf = parts[1].substring(0, parts[1].length() - 1).replace('.', '/');

					String buk = deobfClass(obf, obf2Buk);
					if (buk == null) {
						continue;
					}

					currentClass = buk;
				} else if (currentClass != null) {
					Matcher matcher = MEMBER_PATTERN.matcher(line);
					matcher.find();

					String obf = matcher.group(3);
					String nameDesc = matcher.group(2);
					if (!nameDesc.contains("(")) {
						if (nameDesc.equals(obf) || nameDesc.contains("$")) {
							continue;
						}
						if (!includeMethods && (obf.equals("if") || obf.equals("do"))) {
							obf += "_";
						}

						outFields.add(currentClass + " " + obf + " " + nameDesc);
					} else if (includeMethods) {
						String sig = csrgDesc(moj2Obf, obf2Buk, nameDesc.substring(nameDesc.indexOf('(')), matcher.group(1));
						String mojName = nameDesc.substring(0, nameDesc.indexOf('('));

						if (obf.equals(mojName) || mojName.contains("$") || obf.equals("<init>") || obf.equals("<clinit>")) {
							continue;
						}
						outFields.add(currentClass + " " + obf + " " + sig + " " + mojName);
					}
				}
			}

			Collections.sort(outFields);
			fields.createNewFile();
			Files.write(fields.toPath(), outFields);
		}

		public void makeCombinedMaps(File out, File... members) throws IOException {
			List<String> combined = new ArrayList<>(header);

			for (Map.Entry<String, String> map : obf2Buk.entrySet()) {
				combined.add(map.getKey() + " " + map.getValue());
			}

			for (File member : members) {
				for (String line : Files.readAllLines(member.toPath())) {
					if (line.startsWith("#")) {
						continue;
					}
					line = line.trim();

					String[] split = line.split(" ");
					if (split.length == 3) {
						String clazz = split[0];
						String orig = split[1];
						String targ = split[2];

						combined.add(deobfClass(clazz, obf2Buk.inverse()) + " " + orig + " " + targ);
					} else if (split.length == 4) {
						String clazz = split[0];
						String orig = split[1];
						String desc = split[2];
						String targ = split[3];

						combined.add(deobfClass(clazz, obf2Buk.inverse()) + " " + orig + " " + toObf(desc, obf2Buk.inverse()) + " " + targ);
					}
				}
			}

			Files.write(out.toPath(), combined);
		}
	}


}
