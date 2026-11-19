package com.pro0101.vaultline.modules.huntbuddy.util;

import com.pro0101.vaultline.VaultlineAddon;

public class LogUtil {
   public static void info(String msg) {
      VaultlineAddon.LOG.info("{} {}", MsgUtil.getRawPrefix(), msg);
   }

   public static void info(String msg, String module) {
      VaultlineAddon.LOG.info("{}{} {}", new Object[]{MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg});
   }

   public static void warn(String msg) {
      VaultlineAddon.LOG.warn("{} {}", MsgUtil.getRawPrefix(), msg);
   }

   public static void warn(String msg, String module) {
      VaultlineAddon.LOG.warn("{}{} {}", new Object[]{MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg});
   }

   public static void error(String msg) {
      VaultlineAddon.LOG.error("{} {}", MsgUtil.getRawPrefix(), msg);
   }

   public static void error(String msg, String module) {
      VaultlineAddon.LOG.error("{}{} {}", new Object[]{MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg});
   }
}
