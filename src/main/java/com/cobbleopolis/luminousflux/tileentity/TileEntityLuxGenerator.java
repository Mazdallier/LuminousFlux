package com.cobbleopolis.luminousflux.tileentity;

import com.cobbleopolis.luminousflux.handler.FuelHandlerLuxGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileEntityLuxGenerator extends TileEntity implements ISidedInventory {

	private static final int[] slotsTop = new int[]{0};
	private static final int[] slotsBottom = new int[]{0};
	private static final int[] slotsSides = new int[]{0};

	private ItemStack[] itemStacks = new ItemStack[1];

	public int burnTime;
	public int maxBurnTime;
	public int storedLux;
	public int maxLux;

	private String generatorName;

	public TileEntityLuxGenerator() {
		super();
		storedLux = 0;
		maxLux = 512;
	}

	public void furnaceName(String string) {
		this.generatorName = string;
	}

	@Override
	public int getSizeInventory() {
		return this.itemStacks.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return this.itemStacks[slot];
	}

	@Override
	public ItemStack decrStackSize(int par1, int par2) {
		if (this.itemStacks[par1] != null) {
			ItemStack itemstack;
			if (this.itemStacks[par1].stackSize <= par2) {
				itemstack = this.itemStacks[par1];
				this.itemStacks[par1] = null;
				return itemstack;
			} else {
				itemstack = this.itemStacks[par1].splitStack(par2);

				if (this.itemStacks[par1].stackSize == 0) {
					this.itemStacks[par1] = null;
				}
				return itemstack;
			}
		} else {
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		if (this.itemStacks[slot] != null) {
			ItemStack itemstack = this.itemStacks[slot];
			this.itemStacks[slot] = null;
			return itemstack;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack itemstack) {
		this.itemStacks[slot] = itemstack;

		if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit()) {
			itemstack.stackSize = this.getInventoryStackLimit();
		}

	}

	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.generatorName : "Lux Generator";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.generatorName != null && this.generatorName.length() > 0;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		NBTTagList tagList = tagCompound.getTagList("items", 10);
		this.itemStacks = new ItemStack[this.getSizeInventory()];

		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound tabCompound1 = tagList.getCompoundTagAt(i);
			byte byte0 = tabCompound1.getByte("slot");

			if (byte0 >= 0 && byte0 < this.itemStacks.length) {
				this.itemStacks[byte0] = ItemStack.loadItemStackFromNBT(tabCompound1);
			}
		}

		this.burnTime = tagCompound.getInteger("burnTime");
		this.maxBurnTime = tagCompound.getInteger("maxBurnTime");
		this.storedLux = tagCompound.getInteger("storedLux");
		this.maxLux = tagCompound.getInteger("maxLux");

		if (tagCompound.hasKey("generatorName", 8)) {
			this.generatorName = tagCompound.getString("generatorName");
		}
	}

	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("burnTime", this.burnTime);
		tagCompound.setInteger("maxBurnTime", this.maxBurnTime);
		tagCompound.setInteger("storedLux", this.storedLux);
		tagCompound.setInteger("maxLux", this.maxLux);
		NBTTagList tagList = new NBTTagList();

		for (int i = 0; i < this.itemStacks.length; ++i) {
			if (this.itemStacks[i] != null) {
				NBTTagCompound tagCompound1 = new NBTTagCompound();
				tagCompound1.setByte("slot", (byte) i);
				this.itemStacks[i].writeToNBT(tagCompound1);
				tagList.appendTag(tagCompound1);
			}
		}

		tagCompound.setTag("items", tagList);

		if (this.hasCustomInventoryName()) {
			tagCompound.setString("generatorName", this.generatorName);
		}
	}

	public int getScaledBurnTime(int i) {
		if (this.maxBurnTime == 0)
			return 0;

		return i * this.burnTime / this.maxBurnTime;
	}

	public int getScaledEnergy(int i) {
		if (this.maxLux == 0)
			return 0;

		return i * this.storedLux / this.maxLux;
	}

	public boolean isBurning() {
		return this.burnTime > 0;
	}

	@Override
	public void updateEntity() {
		boolean makeDirty = false;

		if (!this.worldObj.isRemote) {
			if (this.burnTime > 0) {
				this.burnTime--;
				makeDirty = true;
				if (this.storedLux < this.maxLux)
					this.storedLux++;
			}

			if (this.burnTime == 0 && this.canSmelt() && this.storedLux < this.maxLux) {
				this.burnTime = this.maxBurnTime = getItemBurnTime(this.itemStacks[0]);
				this.maxBurnTime = getItemBurnTime(this.itemStacks[0]);

				if (this.burnTime > 0) {
					if (this.itemStacks[0] != null) {
						--this.itemStacks[0].stackSize;
                        this.worldObj.spawnParticle("fireworksSpark", this.xCoord, this.yCoord, this.zCoord, 0, 0, 0);
						if (this.itemStacks[0].stackSize == 0)
							this.itemStacks[0] = itemStacks[0].getItem().getContainerItem(this.itemStacks[0]);
					}
				}
				makeDirty = true;
			}

			if (makeDirty) {
				worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
				this.markDirty();
			}
		}
//		this.printOut();
	}

	public void printOut() {
		System.out.println("Burn Time: " + this.burnTime);
		System.out.println("Max Burn Time: " + this.maxBurnTime);
		System.out.println("Stored Lux: " + this.storedLux);
		System.out.println("Max Lux: " + this.maxLux);
		System.out.println("-----------------------------");
	}

	private boolean canSmelt() {
		if (this.itemStacks[0] == null) {
			return false;
		} else {
			return getItemBurnTime(this.itemStacks[0]) > 0;
		}
	}

//	public void smeltItem() {
//		if (this.canSmelt()) {
//			ItemStack itemstack = CraftingLuxGenerator.smelting().getSmeltingResult(this.itemStacks[0]);
//
//			if (this.itemStacks[2] == null) {
//				this.itemStacks[2] = itemstack.copy();
//			} else if (this.itemStacks[2].getItem() == itemstack.getItem()) {
//				this.itemStacks[2].stackSize += itemstack.stackSize;
//			}
//
//			--this.itemStacks[0].stackSize;
//
//			if (this.itemStacks[0].stackSize >= 0) {
//				this.itemStacks[0] = null;
//			}
//		}
//	}

	public static int getItemBurnTime(ItemStack itemstack) {
		if (itemstack == null) {
			return 0;
		} else {
			Item item = itemstack.getItem();

			return FuelHandlerLuxGenerator.getItemFuelValue(item);

//			if (item instanceof ItemBlock && Block.getBlockFromItem(item) != Blocks.air) {
//				Block block = Block.getBlockFromItem(item);
//
//				if (block == LFBlocks.glowingGlass) {
//					return 200;
//				}
//			}
//
//			if (item == LFItems.itemBulb) return 1600;
//			if (item instanceof ItemTool && ((ItemTool) item).getToolMaterialName().equals("EMERALD")) return 300;
//			return GameRegistry.getFuelValue(itemstack);
		}
	}

	public static boolean isItemFuel(ItemStack itemstack) {
		return getItemBurnTime(itemstack) > 0;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public void openInventory() {

	}

	@Override
	public void closeInventory() {

	}

	@Override
	public boolean isItemValidForSlot(int par1, ItemStack itemstack) {
		return par1 == 2 ? false : (par1 == 1 ? isItemFuel(itemstack) : true);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int par1) {
		return par1 == 0 ? slotsBottom : (par1 == 1 ? slotsTop : slotsSides);
	}

	@Override
	public boolean canInsertItem(int par1, ItemStack itemstack, int par3) {
		return this.isItemValidForSlot(par1, itemstack);
	}

	@Override
	public boolean canExtractItem(int par1, ItemStack itemstack, int par3) {
		return par3 != 0 || par1 != 1 || itemstack.getItem() == Items.bucket;
	}


	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		this.writeToNBT(tag);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		readFromNBT(packet.func_148857_g());
	}

}