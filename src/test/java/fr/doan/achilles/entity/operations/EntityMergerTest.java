package fr.doan.achilles.entity.operations;

import static javax.persistence.CascadeType.MERGE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import mapping.entity.CompleteBean;
import mapping.entity.UserBean;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Factory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import fr.doan.achilles.dao.GenericDynamicCompositeDao;
import fr.doan.achilles.entity.EntityHelper;
import fr.doan.achilles.entity.manager.CompleteBeanTestBuilder;
import fr.doan.achilles.entity.metadata.EntityMeta;
import fr.doan.achilles.entity.metadata.JoinProperties;
import fr.doan.achilles.entity.metadata.PropertyMeta;
import fr.doan.achilles.entity.metadata.PropertyType;
import fr.doan.achilles.proxy.builder.EntityProxyBuilder;
import fr.doan.achilles.proxy.interceptor.JpaEntityInterceptor;

/**
 * EntityMergerTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityMergerTest
{

	@InjectMocks
	private EntityMerger merger;

	@Mock
	private EntityPersister persister;

	@Mock
	private EntityProxyBuilder interceptorBuilder;

	@Mock
	private JpaEntityInterceptor<Long> interceptor;

	@Mock
	private EntityMeta<Long> entityMeta;

	@Mock
	private PropertyMeta<?, String> propertyMeta;

	@Mock
	private Map<Method, PropertyMeta<?, ?>> dirtyMap;

	@Mock
	private GenericDynamicCompositeDao<Long> dao;

	@Mock
	private EntityHelper helper;

	@Mock
	private Bean entity;

	@Test
	public void should_persist_if_not_proxy() throws Exception
	{
		CompleteBean entity = CompleteBeanTestBuilder.builder().id(1L).name("name").buid();

		when(interceptorBuilder.build(entity, entityMeta)).thenReturn(entity);

		CompleteBean mergedEntity = merger.mergeEntity(entity, entityMeta);

		assertThat(mergedEntity).isSameAs(entity);

		verify(persister).persist(entity, entityMeta);
	}

	@Test
	public void should_merge_proxy_with_simple_dirty() throws Exception
	{
		Factory factory = (Factory) entity;
		when(helper.isProxy(entity)).thenReturn(true);
		when(factory.getCallback(0)).thenReturn(interceptor);
		when(entityMeta.getEntityDao()).thenReturn(dao);

		Method ageSetter = CompleteBean.class.getDeclaredMethod("setAge", Long.class);
		Map<Method, PropertyMeta<?, ?>> dirty = new HashMap<Method, PropertyMeta<?, ?>>();
		dirty.put(ageSetter, propertyMeta);

		when(interceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(dirtyMap.entrySet()).thenReturn(dirty.entrySet());
		when(interceptor.getKey()).thenReturn(1L);
		when(interceptor.getTarget()).thenReturn(entity);
		when(propertyMeta.type()).thenReturn(PropertyType.SIMPLE);

		CompleteBean mergedEntity = merger.mergeEntity(entity, entityMeta);

		assertThat(mergedEntity).isSameAs(entity);

		verify(persister).persistProperty(entity, 1L, dao, propertyMeta);
		verify(dirtyMap).clear();
	}

	@Test
	public void should_merge_proxy_with_multi_value_dirty() throws Exception
	{
		Factory factory = (Factory) entity;
		when(helper.isProxy(entity)).thenReturn(true);
		when(factory.getCallback(0)).thenReturn(interceptor);
		when(entityMeta.getEntityDao()).thenReturn(dao);

		Method ageSetter = CompleteBean.class.getDeclaredMethod("setAge", Long.class);
		Map<Method, PropertyMeta<?, ?>> dirty = new HashMap<Method, PropertyMeta<?, ?>>();
		dirty.put(ageSetter, propertyMeta);

		when(interceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(dirtyMap.entrySet()).thenReturn(dirty.entrySet());
		when(interceptor.getKey()).thenReturn(1L);
		when(interceptor.getTarget()).thenReturn(entity);
		when(propertyMeta.type()).thenReturn(PropertyType.LAZY_SET);

		CompleteBean mergedEntity = merger.mergeEntity(entity, entityMeta);

		assertThat(mergedEntity).isSameAs(entity);

		verify(persister).removeProperty(1L, dao, propertyMeta);
		verify(persister).persistProperty(entity, 1L, dao, propertyMeta);
		verify(dirtyMap).clear();
	}

	@Test
	public void should_merge_proxy_with_no_dirty() throws Exception
	{
		Factory factory = (Factory) entity;
		when(helper.isProxy(entity)).thenReturn(true);
		when(factory.getCallback(0)).thenReturn(interceptor);
		when(entityMeta.getEntityDao()).thenReturn(dao);

		Map<Method, PropertyMeta<?, ?>> dirty = new HashMap<Method, PropertyMeta<?, ?>>();

		when(interceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(dirtyMap.entrySet()).thenReturn(dirty.entrySet());

		CompleteBean mergedEntity = merger.mergeEntity(entity, entityMeta);

		assertThat(mergedEntity).isSameAs(entity);

		verifyZeroInteractions(persister);
		verify(dirtyMap).clear();
	}

	@Test
	public void should_merge_proxy_with_join_entity() throws Exception
	{
		Factory factory = (Factory) entity;
		when(helper.isProxy(entity)).thenReturn(true);
		when(factory.getCallback(0)).thenReturn(interceptor);
		when(entityMeta.getEntityDao()).thenReturn(dao);
		Map<Method, PropertyMeta<?, ?>> dirty = new HashMap<Method, PropertyMeta<?, ?>>();

		when(interceptor.getDirtyMap()).thenReturn(dirtyMap);
		when(dirtyMap.entrySet()).thenReturn(dirty.entrySet());

		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();
		JoinProperties joinProperties = new JoinProperties();
		joinProperties.setEntityMeta(joinEntityMeta);
		joinProperties.addCascadeType(MERGE);

		Method userGetter = Bean.class.getMethod("getUser");
		Method userSetter = Bean.class.getMethod("setUser", UserBean.class);

		PropertyMeta<Void, UserBean> joinPropertyMeta = new PropertyMeta<Void, UserBean>();
		joinPropertyMeta.setType(PropertyType.JOIN_SIMPLE);
		joinPropertyMeta.setSingleKey(true);

		joinPropertyMeta.setJoinProperties(joinProperties);
		joinPropertyMeta.setGetter(userGetter);
		joinPropertyMeta.setSetter(userSetter);

		Map<String, PropertyMeta<?, ?>> propertyMetaMap = new HashMap<String, PropertyMeta<?, ?>>();
		propertyMetaMap.put("joinEntity", joinPropertyMeta);

		when(entityMeta.getPropertyMetas()).thenReturn(propertyMetaMap);

		UserBean userBean = new UserBean();

		when(helper.getValueFromField(entity, userGetter)).thenReturn(userBean);
		when(interceptorBuilder.build(userBean, joinEntityMeta)).thenReturn(userBean);

		merger.mergeEntity(entity, entityMeta);

		verify(persister).persist(userBean, joinEntityMeta);
		verify(helper).setValueToField(entity, userSetter, userBean);

	}

	class Bean extends CompleteBean implements Factory
	{
		public static final long serialVersionUID = 1L;

		@Override
		public Object newInstance(Callback callback)
		{
			return null;
		}

		@Override
		public Object newInstance(Callback[] callbacks)
		{
			return null;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object newInstance(Class[] types, Object[] args, Callback[] callbacks)
		{
			return null;
		}

		@Override
		public Callback getCallback(int index)
		{
			return null;
		}

		@Override
		public void setCallback(int index, Callback callback)
		{}

		@Override
		public void setCallbacks(Callback[] callbacks)
		{}

		@Override
		public Callback[] getCallbacks()
		{
			return null;
		}

	}
}
