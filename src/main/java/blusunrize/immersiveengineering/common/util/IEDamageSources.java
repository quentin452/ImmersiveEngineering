package blusunrize.immersiveengineering.common.util;

import blusunrize.immersiveengineering.common.entities.EntityRailgunShot;
import blusunrize.immersiveengineering.common.entities.EntityRevolvershot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

public class IEDamageSources
{
	public static class IEDamageSource_Indirect extends EntityDamageSourceIndirect
	{
		public IEDamageSource_Indirect(String tag, Entity shot, Entity shooter)
		{
			super(tag, shot, shooter);
		}
	}
	public static class IEDamageSource_Direct extends EntityDamageSource
	{
		public IEDamageSource_Direct(String tag, Entity attacker)
		{
			super(tag, attacker);
		}
	}
	public static class IEDamageSource extends DamageSource
	{
		public IEDamageSource(String tag)
		{
			super(tag);
		}
	}

	public static DamageSource causeCasullDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverCasull, shot, shooter);
	}
	public static DamageSource causePiercingDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverAP, shot, shooter).setDamageBypassesArmor();
	}
	public static DamageSource causeBuckshotDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverBuck, shot, shooter);
	}
	public static DamageSource causeDragonsbreathDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverDragon, shot, shooter).setFireDamage();
	}
	public static DamageSource causeHomingDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverHoming, shot, shooter);
	}
	public static DamageSource causeWolfpackDamage(EntityRevolvershot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverWolfpack, shot, shooter);
	}
	public static DamageSource causeSilverDamage(EntityRevolvershot shot, Entity shooter)
	{
		EntityDamageSourceIndirect silver = new EntityDamageSourceIndirect("indirectMagic", shot, shooter);
		silver.setProjectile();
		silver.damageType = Lib.DMG_RevolverSilver;
		return silver;
	}
	public static DamageSource causePotionDamage(EntityRevolvershot shot, EntityLivingBase shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_RevolverPotion, shot, shooter);
	}

	public static DamageSource causeAcidDamage()
	{
		return new IEDamageSource(Lib.DMG_Acid);
	}

	public static DamageSource causeCrusherDamage()
	{
		return new IEDamageSource(Lib.DMG_Crusher);
	}
	
	public static DamageSource causeRailgunDamage(EntityRailgunShot shot, Entity shooter)
	{
		return new IEDamageSource_Indirect(Lib.DMG_Railgun, shot, shooter).setDamageBypassesArmor();
	}
}