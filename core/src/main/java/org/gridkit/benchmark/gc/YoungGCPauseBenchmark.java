/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.benchmark.gc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.gridkit.jvmtool.GCSelfMonitor;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class YoungGCPauseBenchmark {

	private static Map<Integer, Object> maps[];
	private static Random random = new Random();
	
//	private static int ENTRY_SIZE = Integer.getInteger("gc-measure.entry-size", 104); // 64bit
	private static int ENTRY_SIZE = Integer.getInteger("gc-measure.entry-size", 62); // 32bit
	private static int HEADROOM = Integer.getInteger("gc-measure.headroom", 256);
	private static boolean STRING_MODE = Boolean.getBoolean("gc-measure.string-mode");
	private static int STRING_LEN = Integer.getInteger("gc-measure.string-length", 64);
	
	private static boolean DRY_MODE = Boolean.getBoolean("gc-measure.dry-mode");
	private static boolean CMS_MODE = Boolean.getBoolean("gc-measure.cms-mode");
//	private static boolean REPORT_CPU = Boolean.getBoolean("gc-measure.report-cpu");
	
	private static char[] STRING_TEMPLATE = new char[STRING_LEN];
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
//		if (REPORT_CPU) {
//			CpuUsageReporter.startReporter(System.out, 15000);
//		}
		
		long tenuredSize = Runtime.getRuntime().maxMemory();
		// getting more accurate data
		for(MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
			if ("Tenured Gen".equals(bean.getName()) || "PS Old Gen".equals(bean.getName()) || "CMS Old Gen".equals(bean.getName())) {				
				tenuredSize = bean.getUsage().getMax();
				System.out.println("Exact tenured space size is " + tenuredSize);
			}
		}
		
		long limit = 0;
		if (args.length == 1) {
			limit = Long.parseLong(args[0]);
			System.out.println("Runlimit: " + limit + "secs");
			limit = TimeUnit.SECONDS.toMillis(limit);
		}
		
		if (CMS_MODE) {
			HEADROOM = 0;
		}
		
		int count = (int) ((tenuredSize - (HEADROOM << 20)) / ENTRY_SIZE);
		if (CMS_MODE) {
			count /= 2;			
			System.out.println("CMS mode is enabled");
		}
		System.out.println("Total old space: " + (tenuredSize >> 20) + "M (-" + HEADROOM + "M)");
		if (count < 0) {
			System.out.println("Heap size is too small, increase heap size or reduce headroom");
		}
		System.out.println("Populating - " + count);
		maps = new Map[(count + 200000 -1) / 200000];
		for(int i = 0; i != maps.length; ++i) {
			maps[i] = new HashMap<Integer, Object>();
		}
 		while(size() < count) {
			putRandom(count);
			if (CMS_MODE & random.nextInt(10) > 7) {
				removeRandom(count);
			}
		}
 		int n = 0;
 		int sz = 0;
 		if (!DRY_MODE) {
 			System.out.println("Processing ...");
 		}
 		else {
 			System.out.println("Processing ... (DRY MODE ENABLED)");
 		}
 		
 		GCSelfMonitor gcmonitor = new GCSelfMonitor();
 		
 		// start count down here
 		long startTime = System.currentTimeMillis();
		while(true) {
			if (n % 100 == 0) {
				sz = size();
			}
			++n;
			if ((sz < 1.01 * count) && random.nextBoolean()) {
				if (DRY_MODE) {
					dryPutRandom(count);
				}
				else {
					putRandom(count);
				}
			}
			else {
				if (DRY_MODE) {
					dryRemoveRandom(count);
				}
				else {
					removeRandom(count);
				}
			}
			if (limit != 0 && (System.currentTimeMillis() > (startTime + limit))) {
				System.out.println("Finished");
				break;
			}
		}
		
		gcmonitor.displayStats();
	}

	@SuppressWarnings("rawtypes")
	private static int size() {
		int size = 0;
		for(Map map: maps) {
			size += map.size();
		}
		return size;
	}
	
	private static void putRandom(int count) {
		int key = random.nextInt(2 * count);
		Object val = STRING_MODE ? new String(STRING_TEMPLATE) : new Integer(random.nextInt());
		maps[key % maps.length].put(key, val);
	}

	private static void dryPutRandom(int count) {
		int key = random.nextInt(2 * count);
		Object val = STRING_MODE ? new String(STRING_TEMPLATE) : new Integer(random.nextInt());
		val.equals(maps[key % maps.length].get(key));
	}

	private static void removeRandom(int count) {
		int key = random.nextInt(2 * count);
		maps[key % maps.length].remove(key);
	}

	private static void dryRemoveRandom(int count) {
		int key = random.nextInt(2 * count);
		maps[key % maps.length].get(key);
	}
}
