package com.codex.mobspawnerinjector.util;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.codex.mobspawnerinjector.util.CustomMobSpawner.a;
import com.google.common.collect.Lists;

import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityMinecartAbstract;
import net.minecraft.server.v1_8_R3.EntityTypes;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.MobSpawnerAbstract;
import net.minecraft.server.v1_8_R3.NBTBase;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.UtilColor;
import net.minecraft.server.v1_8_R3.WeightedRandom;

@SuppressWarnings("unused")
public abstract class CustomMobSpawner extends MobSpawnerAbstract {

	public int spawnDelay = 20;
	
	private String mobName = "Zombie";
	private final List<a> mobs = Lists.newArrayList();
	private a spawnData;
	private double e;
	private double f;
	private int minSpawnDelay = 200;
	private int maxSpawnDelay = 800;
	private int spawnCount = 4;
	private int maxNearbyEntities = 6;
	private int requiredPlayerRange = 32;
	private int spawnRange = 4;
	
	// ALWAYS RUNNING (TICK)
	@Override
	public void c() {
		//If player is in range
		if (g()) {
			BlockPosition blockposition = b();
			
			//CLIENT SIDE (Changing/removing this has no effect)
			if (a().isClientSide) {
				double d1 = blockposition.getX() + a().random.nextFloat();
				double d2 = blockposition.getY() + a().random.nextFloat();

				double d0 = blockposition.getZ() + a().random.nextFloat();
				a().addParticle(EnumParticle.SMOKE_NORMAL, d1, d2, d0, 0.0D, 0.0D, 0.0D, new int[0]);
				a().addParticle(EnumParticle.FLAME, d1, d2, d0, 0.0D, 0.0D, 0.0D, new int[0]);
				if (this.spawnDelay > 0) {
					this.spawnDelay -= 1;
				}
				this.f = this.e;
				this.e = ((this.e + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0D);
			} 
			//Server side (Stuff we want to edit/change)
			else {
				//Main tick
				
				//Manage spawn delay (decrement)
				if (this.spawnDelay == -1) {
					h();
				}
				if (this.spawnDelay > 0) {
					this.spawnDelay -= 1;
					return;
				}
				
				//Loops for every spawn count (I believe this is the maximum number of mobs the spawner can spawn) -- set to 4
				for (int i = 0; i < this.spawnCount; i++) {
					//Create entity based on spawner type
					Entity entity = EntityTypes.createEntityByName(getMobName(), a());
					
					if (entity == null) {
						return;
					}

					AxisAlignedBB axisAligned = new AxisAlignedBB(blockposition.getX(), blockposition.getY(), blockposition.getZ(),
							blockposition.getX() + 1, blockposition.getY() + 1, blockposition.getZ() + 1)
									.grow(this.spawnRange, this.spawnRange, this.spawnRange);
					
					//Number of nearby entities (using methods from World class)
					int nearbyEntityCount = a().a(entity.getClass(),axisAligned).size();
					
					//If the number of entities nearby is greater then the maximum then don't spawn (return)
					if (nearbyEntityCount >= this.maxNearbyEntities) {
						h();
						return;
					}
					
					//Get random location within spawn range to spawn the mob
					double randomX = blockposition.getX()
							+ (a().random.nextDouble() - a().random.nextDouble()) * this.spawnRange + 0.5D;
					double randomY = blockposition.getY() + a().random.nextInt(3) - 1;
					double randomZ = blockposition.getZ()
							+ (a().random.nextDouble() - a().random.nextDouble()) * this.spawnRange + 0.5D;
					
					//Check if the entity can be casted to EntityInsentient
					EntityInsentient entityinsentient = (entity instanceof EntityInsentient) ? (EntityInsentient) entity : null;

					//Set entity location to the spawn location (entity object is created already) 
					//NOTE: This does not actually "spawn" the entity. 
					//It only sets the location of the entity object to the desired location.
					entity.setPositionRotation(randomX, randomY, randomZ, a().random.nextFloat() * 360.0F, 0.0F);
					
					//If entity is actually a spawnable entity
					if ((entityinsentient == null) || ((entityinsentient.bR()) && (entityinsentient.canSpawn()))) {
						
						// Runs the a method -- (Refer to a)
						a(entity, true);

						// Firework when the entity spawns
						Location loc = new Location(entity.getWorld().getWorld(), (double) blockposition.getX(),
								(double) blockposition.getY(), (double) blockposition.getZ());
						
						FireworkUtil.spawnRandomFirework(loc);

						//This code is for the smoke and flame particles. TODO: Replace with custom particles
						ParticleUtil.createSpawnerParticleEffectAtLocation(loc);
						
						
						// a().triggerEffect(2004, blockposition, 0);
						//if (entityinsentient != null) {
							// entityinsentient.y();
						//}
				
						h();
					}
				}
			}
		}
	}
	
	// If player in range
	public boolean g() {
		BlockPosition blockposition = b();
		return a().isPlayerNearby(blockposition.getX() + 0.5D, blockposition.getY() + 0.5D, blockposition.getZ() + 0.5D,
				this.requiredPlayerRange);
	}

	//Spawn delay method
	private void h() {
		if (this.maxSpawnDelay <= this.minSpawnDelay) {
			this.spawnDelay = this.minSpawnDelay;
		} else {
			int i = this.maxSpawnDelay - this.minSpawnDelay;

			this.spawnDelay = (this.minSpawnDelay + a().random.nextInt(i));
		}
		if (this.mobs.size() > 0) {
			this.a((a) WeightedRandom.a(a().random, this.mobs));
		}
		a(1);
	}

	//Spawn the entity method
	private Entity a(Entity entity, boolean flag) {
		//If spawner's spawn data isn't null
		if (i() != null) {
			NBTTagCompound nbttagcompound = new NBTTagCompound();

			//Set entity ID to the nbttag then populate the tag with all the other information
			entity.d(nbttagcompound);
			
			//Iterator for all of the NBTBase in the spawners NBTCompound
			Iterator<String> iterator = i().c.c().iterator();
			
			//Loop all of them
			while (iterator.hasNext()) {
				//Key saved in map
				String s = (String) iterator.next();
				//NBTBase from the key
				NBTBase nbtbase = i().c.get(s);
				
				//set all the data to the newly created map.
				nbttagcompound.set(s, nbtbase.clone());
			}

			//Loads NBT data to the entity from the NBT compound
			entity.f(nbttagcompound);
			
			//Add entity to the world (Spawns a visible entity)
			if ((entity.world != null) && (flag)) {
				entity.world.addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);
			}

			NBTTagCompound spawnData = new NBTTagCompound();
			// Populate spawnData with spawners data
			b(spawnData);
			entity.setCustomName(spawnData.getString("MobDisplayName"));

			//Compound for riding entities (Jockey)
			NBTTagCompound nbttagcompound1;

			// Riding entities (Jockey)
			for (Entity entity1 = entity; nbttagcompound.hasKeyOfType("Riding", 10); nbttagcompound = nbttagcompound1) {
				nbttagcompound1 = nbttagcompound.getCompound("Riding");
				Entity entity2 = EntityTypes.createEntityByName(nbttagcompound1.getString("id"), entity.world);
				if (entity2 != null) {
					NBTTagCompound nbttagcompound2 = new NBTTagCompound();

					entity2.d(nbttagcompound2);
					Iterator<String> iterator1 = nbttagcompound1.c().iterator();
					while (iterator1.hasNext()) {
						String s1 = (String) iterator1.next();
						NBTBase nbtbase1 = nbttagcompound1.get(s1);

						nbttagcompound2.set(s1, nbtbase1.clone());
					}
					entity2.f(nbttagcompound2);
					entity2.setPositionRotation(entity1.locX, entity1.locY, entity1.locZ, entity1.yaw, entity1.pitch);
					if ((entity.world != null) && (flag)) {
						entity.world.addEntity(entity2, CreatureSpawnEvent.SpawnReason.SPAWNER);
					}
					entity1.mount(entity2);
				}
				entity1 = entity2;
			}
		}
		//If the spawners data is null but the entity should still spawn 
		else if (((entity instanceof EntityLiving)) && (entity.world != null) && (flag)) {
			if ((entity instanceof EntityInsentient)) {
				((EntityInsentient) entity).prepare(entity.world.E(new BlockPosition(entity)), null);
			}
			//Spawns entity
			entity.world.addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);

			NBTTagCompound spawnData = new NBTTagCompound();
			// Populate spawnData with spawners data
			b(spawnData);
			entity.setCustomName(spawnData.getString("MobDisplayName"));
		}
		return entity;
	}

	public void b(NBTTagCompound nbttagcompound) {
		String s = getMobName();
		if (!UtilColor.b(s)) {
			nbttagcompound.setString("EntityId", mobName);
			nbttagcompound.setString("MobDisplayName", "�3�lMobby McMobface");
			nbttagcompound.setShort("Delay", (short) this.spawnDelay);
			nbttagcompound.setShort("MinSpawnDelay", (short) this.minSpawnDelay);
			nbttagcompound.setShort("MaxSpawnDelay", (short) this.maxSpawnDelay);
			nbttagcompound.setShort("SpawnCount", (short) this.spawnCount);
			nbttagcompound.setShort("MaxNearbyEntities", (short) this.maxNearbyEntities);
			nbttagcompound.setShort("RequiredPlayerRange", (short) this.requiredPlayerRange);
			nbttagcompound.setShort("SpawnRange", (short) this.spawnRange);
			if (i() != null) {
				nbttagcompound.set("SpawnData", i().c.clone());
			}
			if ((i() != null) || (this.mobs.size() > 0)) {
				NBTTagList nbttaglist = new NBTTagList();
				if (this.mobs.size() > 0) {
					Iterator<a> iterator = this.mobs.iterator();
					while (iterator.hasNext()) {
						a mobspawnerabstract_a = (a) iterator.next();

						nbttaglist.add(mobspawnerabstract_a.a());
					}
				} else {
					nbttaglist.add(i().a());
				}
				nbttagcompound.set("SpawnPotentials", nbttaglist);
			}
		}
	}

	public void a(a mobspawnerabstract_a) {
		this.spawnData = mobspawnerabstract_a;
	}

	private a i() {
		return this.spawnData;
	}
	
	public String getMobName() {
		if (i() == null) {
			if (this.mobName == null) {
				this.mobName = "Pig";
			}
			if ((this.mobName != null) && (this.mobName.equals("Minecart"))) {
				this.mobName = "MinecartRideable";
			}
			return this.mobName;
		}
		return i().d;
	}

	public class a extends WeightedRandom.WeightedRandomChoice {
		
		private final NBTTagCompound c;
		private final String d;

		public a(NBTTagCompound nbttagcompound) {
			this(nbttagcompound.getCompound("Properties"), nbttagcompound.getString("Type"),
					nbttagcompound.getInt("Weight"));
		}

		public a(NBTTagCompound nbttagcompound, String s) {
			this(nbttagcompound, s, 1);
		}

		private a(NBTTagCompound nbttagcompound, String s, int i) {
			super(i);
			if (s.equals("Minecart")) {
				if (nbttagcompound != null) {
					s = EntityMinecartAbstract.EnumMinecartType.a(nbttagcompound.getInt("Type")).b();
				} else {
					s = "MinecartRideable";
				}
			}
			this.c = nbttagcompound;
			this.d = s;
		}

		public NBTTagCompound a() {
			NBTTagCompound nbttagcompound = new NBTTagCompound();

			nbttagcompound.set("Properties", this.c);
			nbttagcompound.setString("Type", this.d);
			nbttagcompound.setInt("Weight", this.a);
			return nbttagcompound;
		}
	}
}
