package net.dmulloy2.swornguns.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryHelper {
  public static int amtItem(Inventory inventory, int itemid, byte dat) {
	  int ret = 0;
	  if (inventory != null) {
		  ItemStack[] items = inventory.getContents();
		  for (int slot = 0; slot < items.length; slot++) {
			  if (items[slot] != null) {
				  int id = items[slot].getTypeId();
				  int itmDat = items[slot].getData().getData();
				  int amt = items[slot].getAmount();
				  if ((id == itemid) && ((dat == itmDat) || (dat == -1))) {
					  ret += amt;
				  }
			  }
		  }
	  }
	  return ret;
  }

  public static void removeItem(Inventory inventory, int itemid, byte dat, int amt) {
	  int start = amt;
	  if (inventory != null) {
		  ItemStack[] items = inventory.getContents();
		  for (int slot = 0; slot < items.length; slot++) {
			  if (items[slot] != null) {
				  int id = items[slot].getTypeId();
				  int itmDat = items[slot].getData().getData();
				  int itmAmt = items[slot].getAmount();
				  if ((id == itemid) && ((dat == itmDat) || (dat == -1))) {
					  if (amt > 0) {
						  if (itmAmt >= amt) {
							  itmAmt -= amt;
							  amt = 0;
						  } else {
							  amt = start - itmAmt;
							  itmAmt = 0;
						  }
						  if (itmAmt > 0) {
							  inventory.getItem(slot).setAmount(itmAmt);
						  } else {
							  inventory.setItem(slot, null);
						  }
					  }
					  if (amt <= 0)
						  return;
				  }
			  }
		  }
	  }
  }
}