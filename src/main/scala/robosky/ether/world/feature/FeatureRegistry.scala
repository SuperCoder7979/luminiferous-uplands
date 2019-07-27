package robosky.ether.world.feature

import com.mojang.datafixers.Dynamic
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.gen.feature.{DefaultFeatureConfig, Feature}
import robosky.ether.world.feature.trees.SkyrootTreeFeature

object FeatureRegistry {
  val oreFeature: EtherOreFeature.type = register("oregen", EtherOreFeature)
  val skyrootTreeFeature: SkyrootTreeFeature = register("skyroot_tree", new SkyrootTreeFeature((t: Dynamic[_]) => DefaultFeatureConfig.deserialize(t), false));

  def register[A <: Feature[_]](name: String, f: A): A = Registry.register[A](Registry.FEATURE, new Identifier("ether_dim", name), f)

  def init(): Unit = {}
}