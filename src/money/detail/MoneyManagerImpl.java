﻿package money.detail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork2.ActionContext;

import net.sf.json.JSONObject;
import brightmoon.jdbc.DataHandler;
import brightmoon.jdbc.DbTool;
import brightmoon.util.NewJsonUtil;

import common.base.AllSelect;
import common.base.ParamSelect;
import common.base.SpringContextUtil;
import common.util.CommonUtil;
import common.util.DateTool;
import common.util.NPOIReader;

import dwz.constants.BeanManagerKey;
import dwz.framework.constants.Constants;
import dwz.framework.constants.user.UserType;
import dwz.framework.core.business.AbstractBusinessObjectManager;
import dwz.framework.core.exception.ValidateFieldsException;
import dwz.framework.user.impl.UserImpl;

public class MoneyManagerImpl extends AbstractBusinessObjectManager implements
		MoneyManager {
	String addMoneySql = "insert into money_detail_t( money_time,money,money_type,"
			+ "money_desc,useful,code)" + " values(   ? ,?,?,?,1,? )";
	String queryMoneySql = "select money_sno,money_time,money,money_type,money_desc from money_detail_t where code=? limit ?,?";
	String queryMoneyCountSql = "select count(1) from money_detail_t where code=?  ";
	String updateMoneySql = "update money_detail_t set code = ?  where code=?";
	String groupByYear = "select tt.y,sum(tt.money),tt.tp,tt.des from ("
			+ " select year(t.money_Time) y,t.money,tp.tally_type_sno tp,tp.tally_type_desc des "
			+ " from money_detail_t as t,tally_type_t as tp,tally_type_t as tp2"
			+ " where tp2.parent_code= tp.type_code and  t.money_Type = tp2.type_code ) as tt "
			+ "group by tt.y,tt.tp,tt.des order by tt.y";
	String groupByMonth = "select tt.y,tt.m,sum(tt.money),tt.tp,tt.des from ("
			+ "  select year(t.money_Time) y,month(t.money_time) m,t.money,tp.tally_type_sno tp,tp.tally_type_desc des "
			+ "  from money_detail_t as t,tally_type_t as tp,tally_type_t as tp2"
			+ "  where tp2.parent_code= tp.type_code and  t.money_Type = tp2.type_code and year(t.money_Time) = ? ) as tt "
			+ "  group by tt.m,tt.tp,tt.des order by tt.m";
	private MoneyDao moneyDao = null;

	public MoneyManagerImpl(MoneyDao moneyDao) {
		this.moneyDao = moneyDao;
	}

	public Integer searchMoneyNum(Map<MoneySearchFields, Object> criterias) {
		if (criterias == null) {
			return 0;
		}
		Object[] quertParas = this.createQuery(true, criterias, null);
		String hql = quertParas[0].toString();
		Number totalCount = this.moneyDao.countByQuery(hql,
				(Object[]) quertParas[1]);

		return totalCount.intValue();
	}

	public Collection<Money> searchMoney(
			Map<MoneySearchFields, Object> criterias, String orderField,
			int startIndex, int count) {
		ArrayList<Money> eaList = new ArrayList<Money>();
		if (criterias == null)
			return eaList;

		Object[] quertParas = this.createQuery(false, criterias, orderField);
		String hql = quertParas[0].toString();
		// 直接根据hql语句进行查询.
		Collection<MoneyVO> voList = this.moneyDao.findByQuery(hql,
				(Object[]) quertParas[1], startIndex, count);

		if (voList == null || voList.size() == 0)
			return eaList;
		AllSelect allSelect = (AllSelect) SpringContextUtil
				.getBean(BeanManagerKey.allSelectManager.toString());
		ParamSelect select1 = allSelect.getAllMoneyType();

		for (MoneyVO po : voList) {
			po.setMoneyTypeName(select1.getName(po.getMoneyType()));
			eaList.add(new MoneyImpl(po));
		}

		return eaList;
	}

	public static Date getDate(int year, int month, int day) {
		GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day);
		return calendar.getTime();
	}

	private Date[] getDates(int year, int month) {
		Date[] times = new Date[2];
		Date start = getDate(year, month, 1);
		Date end = null;
		if (month == 12) {
			end = getDate(year + 1, 1, 1);
		} else
			end = getDate(year, month + 1, 1);
		times[0] = start;
		times[1] = end;
		return times;
	}

	/**
	 * 根据年份和月份得到开始和结束时间.
	 * 
	 * @param year
	 * @param month
	 * @return
	 */
	private Date[] getDates(String year, String month) {
		Date[] times = new Date[2];
		if ("-1".equals(year) && "-1".equals(month)) {
			return null;
		}
		if ("-1".equals(year))
			year = "";
		if ("-1".equals(month))
			month = "";

		// 如果有年月就返回制定月份数据
		if (CommonUtil.isNotEmpty(year) && CommonUtil.isNotEmpty(month)) {
			return getDates(Integer.parseInt(year), Integer.parseInt(month));
		}
		// 如果只有年度，就返回当前年数据
		else if (CommonUtil.isNotEmpty(year)) {
			Date start = getDate(Integer.parseInt(year), 1, 1);
			Date end = getDate(Integer.parseInt(year) + 1, 1, 1);
			times[0] = start;
			times[1] = end;
		}
		// 默认就返回当前月份数据
		else {
			GregorianCalendar ca = new GregorianCalendar();
			ca.setTime(new Date());
			int iMonth = ca.get(Calendar.MONTH) + 1;
			int iYear = ca.get(Calendar.YEAR);
			return getDates(iYear, iMonth);
		}
		return times;
	}

	private Object[] createQuery(boolean useCount,
			Map<MoneySearchFields, Object> criterias, String orderField) {
		StringBuilder sb = new StringBuilder();
		sb.append(
				useCount ? "select count(distinct money) "
						: "select distinct money ").append(
				"from MoneyVO as money ");

		int count = 0;
		List argList = new ArrayList();
		if (criterias.size() > 0)
			for (Map.Entry<MoneySearchFields, Object> entry : criterias
					.entrySet()) {
				MoneySearchFields fd = entry.getKey();
				switch (fd) {
				case MONEY_SNO:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneySno=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY_TIME:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyTime=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY:
					sb.append(count == 0 ? " where" : " and").append(
							" money.money=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY_TYPE:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyType in ( ");
					String str = "" + entry.getValue();
					String[] tps = str.split(",");
					for (String tp : tps) {
						sb.append("?,");
						argList.add(tp);
					}
					sb = sb.deleteCharAt(sb.lastIndexOf(","));
					sb.append(")");
					count++;
					break;
				case MONEY_DESC:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyDesc like ? ");
					argList.add(entry.getValue());
					count++;
					break;
				case SHOP_CARD:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyCard =? ");
					argList.add(entry.getValue());
					count++;
					break;
				case BOOK_TYPE:
					sb.append(count == 0 ? " where" : " and").append(
							" money.bookType=? ");
					argList.add(entry.getValue());
					count++;
					break;
				default:
					break;
				}
			}
		Date[] times = getDates("" + criterias.get(MoneySearchFields.YEAR), ""
				+ criterias.get(MoneySearchFields.MONTH));
		if (times != null) {
			sb.append(count == 0 ? " where" : " and").append(
					" money.moneyTime>=?  ");
			argList.add(times[0]);
			sb.append(" and  money.moneyTime<?  ");
			argList.add(times[1]);
		}
		if (useCount) {
			return new Object[] { sb.toString(), argList.toArray() };
		}
		MoneyOrderByFields orderBy = MoneyOrderByFields.MONEY_TIME_DESC;
		if (orderField != null && orderField.length() > 0) {
			orderBy = MoneyOrderByFields.valueOf(orderField);
		}

		switch (orderBy) {
		case MONEY_TIME:
			sb.append(" order by money.moneyTime");
			break;
		case MONEY_TYPE:
			sb.append(" order by money.moneyType  ");
			break;
		case MONEY:
			sb.append(" order by money.money");
			break;
		case MONEY_SNO:
			sb.append(" order by money.moneySno  ");
			break;
		case MONEY_TIME_DESC:
			sb.append(" order by money.moneyTime desc");
			break;
		case MONEY_TYPE_DESC:
			sb.append(" order by money.moneyType desc");
			break;
		case MONEY_DESC:
			sb.append(" order by money.money desc");
			break;
		case MONEY_SNO_DESC:
			sb.append(" order by money.moneySno desc");
			break;
		}
		return new Object[] { sb.toString(), argList.toArray() };
	}

	/**
	 * 按照金额小类进行分类总额
	 * 
	 * @param criterias
	 * @returnMoneyTypeVO
	 */
	private Object[] createQueryByTallyType(
			Map<MoneySearchFields, Object> criterias) {
		StringBuilder sb = new StringBuilder();
		sb.append("select sum( money.money),moneyType ").append(
				"from MoneyVO as money ");

		int count = 0;
		List argList = new ArrayList();
		if (criterias.size() > 0)
			for (Map.Entry<MoneySearchFields, Object> entry : criterias
					.entrySet()) {
				MoneySearchFields fd = entry.getKey();
				switch (fd) {
				case MONEY_SNO:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneySno=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY_TIME:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyTime=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY:
					sb.append(count == 0 ? " where" : " and").append(
							" money.money=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY_TYPE:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyType=? ");
					argList.add(entry.getValue());
					count++;
					break;
				case MONEY_DESC:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyDesc like ? ");
					argList.add(entry.getValue());
					count++;
					break;
				case SHOP_CARD:
					sb.append(count == 0 ? " where" : " and").append(
							" money.moneyCard =? ");
					argList.add(entry.getValue());
					count++;
					break;
				case BOOK_TYPE:
					sb.append(count == 0 ? " where" : " and").append(
							" money.bookType=? ");
					argList.add(entry.getValue());
					count++;
					break;
				default:
					break;
				}
			}
		Date[] times = getDates("" + criterias.get(MoneySearchFields.YEAR), ""
				+ criterias.get(MoneySearchFields.MONTH));
		if (times != null) {
			sb.append(count == 0 ? " where" : " and").append(
					" money.moneyTime>=?  ");
			argList.add(times[0]);
			sb.append(" and  money.moneyTime<?  ");
			argList.add(times[1]);
		}

		sb.append(" group by moneyType ");
		return new Object[] { sb.toString(), argList.toArray() };
	}

	/**
	 * 按照全部的金额大类进行区分.
	 * 
	 * @param criterias
	 * @return
	 */
	private Object[] createQueryByMoneyType(
			Map<MoneySearchFields, Object> criterias) {
		StringBuilder sb = new StringBuilder();
		sb.append("select sum( money.money),typeVo.moneyType ")
				.append("from MoneyVO as money,MoneyTypeVO as typeVo where money.moneyType=typeVo.typeCode ");

		List argList = new ArrayList();
		if (criterias.size() > 0)
			for (Map.Entry<MoneySearchFields, Object> entry : criterias
					.entrySet()) {
				MoneySearchFields fd = entry.getKey();
				switch (fd) {
				case MONEY_SNO:
					sb.append(" and  money.moneySno=? ");
					argList.add(entry.getValue());
					break;
				case MONEY_TIME:
					sb.append(" and  money.moneyTime=? ");
					argList.add(entry.getValue());
					break;
				case MONEY:
					sb.append(" and  money.money=? ");
					argList.add(entry.getValue());
					break;
				case MONEY_TYPE:
					String tp = ""+entry.getValue();
					if(tp!=null&&!"null".equals(tp)){
						String[] tps = tp.split(",");
						sb.append(" and  money.moneyType in ( ");
						for(int i=0,j=tps.length;i<j;i++){
							sb.append("?");
							if(i<j-1)
								sb.append(",");
							argList.add(tps[i]);
						}  
						sb.append(" )");
					}
					break;
				case MONEY_DESC:
					sb.append(" and  money.moneyDesc like ? ");
					argList.add(entry.getValue());
					break;
				case SHOP_CARD:
					sb.append(" and  money.moneyCard =? ");
					argList.add(entry.getValue());
					break;
				case BOOK_TYPE:
					sb.append(" and  money.bookType=? ");
					argList.add(entry.getValue());
					break;
				default:
					break;
				}
			}
		Date[] times = getDates("" + criterias.get(MoneySearchFields.YEAR), ""
				+ criterias.get(MoneySearchFields.MONTH));
		if (times != null) {
			sb.append(" and  money.moneyTime>=?  ");
			argList.add(times[0]);
			sb.append(" and  money.moneyTime<?  ");
			argList.add(times[1]);
		}

		sb.append(" group by typeVo.moneyType ");
		return new Object[] { sb.toString(), argList.toArray() };
	}

	public void createMoney(Money money) throws ValidateFieldsException {
		MoneyImpl moneyImpl = (MoneyImpl) money;
		this.moneyDao.insert(moneyImpl.getMoneyVO());
	}

	public void createMoney(Money money, int splitMonths)
			throws ValidateFieldsException {
		UserImpl user = (UserImpl) ActionContext.getContext().getSession()
				.get(Constants.AUTHENTICATION_KEY); 
		String userName = user.getId();  
		MoneyImpl moneyImpl = (MoneyImpl) money;
		MoneyVO v = moneyImpl.getMoneyVO();
		double realMoney = v.getMoney();
		Date time = v.getMoneyTime();
		int maxSplitSno; 
		if (splitMonths > 1) {
			double m = CommonUtil.divide(realMoney, splitMonths, 2);
			Collection<Object> ans = this.moneyDao
					.commonSqlFindMaxSplitSno("select max(splitno) from money_detail_t");
			maxSplitSno = Integer.parseInt(ans.toArray()[0] + "") + 1;
			
			for (int i = 0; i < splitMonths; i++) {
				MoneyVO newV = (MoneyVO) v.clone();
				//设置多个时间.
				newV.setMoneyTime(DateTool.afterAnyDay(time,30*i));
				newV.setMoney(m);
				newV.setRealMoney(realMoney);
				newV.setSplitSno(maxSplitSno); 
				newV.setUserName(userName);
				this.moneyDao.insert(newV);
			}
		}else{
			MoneyVO vv = moneyImpl.getMoneyVO();
			vv.setUserName(userName);
			this.moneyDao.insert(vv);
		}
	}

	public void removeMoney(String moneyId) {
		String[] ids = moneyId.split(",");
		for (String s : ids) {
			MoneyVO vo = this.moneyDao.findByPrimaryKey(Integer.parseInt(s));
			this.moneyDao.delete(vo);
		}
	}

	public void updateMoney(Money money) throws ValidateFieldsException {
		MoneyImpl moneyImpl = (MoneyImpl) money;

		MoneyVO po = moneyImpl.getMoneyVO();
		this.moneyDao.update(po);
	}
	
	public void updateMoney(Money money,int splitMonths) throws ValidateFieldsException {
		MoneyImpl moneyImpl = (MoneyImpl) money;
		MoneyVO v = moneyImpl.getMoneyVO();
		double realMoney = v.getMoney();
		Date time = v.getMoneyTime();
		int maxSplitSno; 
		if (splitMonths > 1) {
			double m = CommonUtil.divide(realMoney, splitMonths, 2);
			Collection<Object> ans = this.moneyDao
					.commonSqlFindMaxSplitSno("select max(splitno) from money_detail_t");
			maxSplitSno = Integer.parseInt(ans.toArray()[0] + "") + 1;
			//先删除老的数据.
			this.moneyDao.delete(v);
			//再插入新的数据
			for (int i = 0; i < splitMonths; i++) {
				MoneyVO newV = (MoneyVO) v.clone();
				//设置多个时间.
				newV.setMoneyTime(DateTool.afterAnyDay(time,30*i));
				newV.setMoney(m);
				newV.setRealMoney(realMoney);
				newV.setSplitSno(maxSplitSno); 
				this.moneyDao.insert(newV);
			}
		}else{
			this.moneyDao.update(v);
		}
	}

	public Money getMoney(Integer id) {
		Collection<MoneyVO> moneys = this.moneyDao.findRecordById(id);

		if (moneys == null || moneys.size() < 1)
			return null;

		MoneyVO money = moneys.toArray(new MoneyVO[moneys.size()])[0];

		AllSelect allSelect = (AllSelect) SpringContextUtil
				.getBean(BeanManagerKey.allSelectManager.toString());
		ParamSelect select1 = allSelect.getAllMoneyType();

		money.setMoneyTypeName(select1.getName(money.getMoneyType()));
		return new MoneyImpl(money);
	}

	public void importFromExcel(File file) {
		NPOIReader excel = null;
		try {
			excel = new NPOIReader(file);
			int index = excel.getSheetNames().indexOf("Sheet0");
			String[][] contents = excel.read(index, true, true);
			for (int i = 1; i < contents.length; i++) {
				MoneyVO vo = new MoneyVO();
				String moneyTimeString = contents[i][0];
				String moneyString = contents[i][1];
				String moneyTypeString = contents[i][2];
				String moneyDescString = contents[i][3];
				vo.setMoneyTime(DateTool.getDate(moneyTimeString));
				vo.setMoney(Double.parseDouble(moneyString));
				vo.setMoneyType(moneyTypeString);
				vo.setMoneyDesc(moneyDescString);
				vo.setShopCard(-1);
				vo.setBookType("1");
				this.moneyDao.insert(vo);
				// this.moneyDao.callProcedure("{call
				// addMoneyDetail(?,?,?,?)}",new
				// Object[]{moneyTimeString,moneyString,moneyTypeString,moneyDescString});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		MoneySyn ss = (MoneySyn) JSONObject.toBean(
				JSONObject.fromObject("{arg1:1}"), MoneySyn.class);
		System.out.println(ss.getArg1());
	}

	public String syn(String method, String json, String data) {
		String result;
		try {
			DbTool db = new DbTool();
			MoneySyn arg = (MoneySyn) NewJsonUtil.jsonToJava(json,
					MoneySyn.class);
			if ("addMonyeFromPhone".equals(method)) {
				String[] moneys = data.split(";");
				String[] ss;
				List momeyVos = new ArrayList();
				for (String s : moneys) {
					s = s.replace("$", "");
					ss = s.split(",");
					momeyVos.clear();
					momeyVos.add(ss[0]);// 日期
					momeyVos.add(ss[1]);// 金额
					momeyVos.add(changeStr(ss[2]));// 描述
					momeyVos.add(changeStr(ss[3]));// 类型
					momeyVos.add(arg.getArg1());
					db.updateRecords(addMoneySql, momeyVos);
				}
				result = "成功添加数目:" + moneys.length + "!";
			} else if ("getAllNewMoneys".equals(method)) {
				List argument = new ArrayList();
				argument.add(arg.getArg1());
				argument.add(Integer.parseInt("" + arg.getArg2()));// 起始行
				argument.add(Integer.parseInt("" + arg.getArg3()));// 终止行
				final StringBuffer buf = new StringBuffer("[");
				db.queryList(queryMoneySql, argument, new DataHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						buf.append("{\"money_time\":\"" + rs.getString(2)
								+ "\",");
						buf.append("\"money\":\"" + rs.getString(3) + "\",");
						buf.append("\"money_type\":\"" + rs.getString(4)
								+ "\",");
						buf.append("\"money_desc\":\"" + rs.getString(5)
								+ "\"},");
					}
				});
				if (buf.lastIndexOf(",") != -1)
					result = buf.deleteCharAt(buf.lastIndexOf(",")).append("]")
							.toString();
				else
					result = "无记录!";
			} else if ("queryMoneyCount".equals(method)) {
				List argument = new ArrayList();
				argument.add(arg.getArg1());
				int count = db.queryForInt(queryMoneyCountSql, argument);
				if (count > 0)
					result = "" + count;
				else
					result = "0";
			} else if ("updateAllNewMoneys".equals(method)) {
				List argument = new ArrayList();
				argument.add(arg.getArg1());
				argument.add(arg.getArg2());
				if (db.updateRecords(updateMoneySql, argument) > 0)
					result = "更新成功!";
				else
					result = "更新失败!";
			} else {
				result = "没有找到合适的处理方法!";
			}
		} catch (Exception e) {
			// PMS Auto-generated catch block
			e.printStackTrace();
			result = "处理失败，请重试!";
		}
		return result;
	}

	private String changeStr(String old) {
		try {
			System.out.println(old);
			System.out.println(new String(old.getBytes("GBK"), "UTF-8"));
			System.out.println(new String(old.getBytes("GBK"), "ISO_8859_1"));
			System.out.println(new String(old.getBytes("UTF-8"), "GBK"));
			System.out.println(new String(old.getBytes("UTF-8"), "ISO_8859_1"));
			System.out.println(new String(old.getBytes("GBK"), "ISO_8859_1"));
			return new String(old.getBytes("GBK"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return old;
		}
	}

	@Override
	public ArrayList<Money> searchMoneyByType(
			Map<MoneySearchFields, Object> criterias) {
		ArrayList<Money> eaList = new ArrayList<Money>();
		if (criterias == null)
			return eaList;

		Object[] quertParas = this.createQueryByMoneyType(criterias);
		String hql = quertParas[0].toString();
		// 直接根据hql语句进行查询.
		Collection<Object[]> voList = this.moneyDao.hibernateSqlFindByType(hql,
				(Object[]) quertParas[1]);

		if (voList == null || voList.size() == 0)
			return eaList;
		for (Object[] po : voList) {
			MoneyVO vo = new MoneyVO();
			vo.setMoney(Double.parseDouble("" + po[0]));
			vo.setMoneyType(po[1] + "");
			eaList.add(new MoneyImpl(vo));
		}

		return eaList;
	}

	public ArrayList<Money> searchMoneyByTallyType(
			Map<MoneySearchFields, Object> criterias) {
		ArrayList<Money> eaList = new ArrayList<Money>();
		if (criterias == null)
			return eaList;

		Object[] quertParas = this.createQueryByTallyType(criterias);
		String hql = quertParas[0].toString();
		// 直接根据hql语句进行查询.
		Collection<Object[]> voList = this.moneyDao.hibernateSqlFindByType(hql,
				(Object[]) quertParas[1]);

		if (voList == null || voList.size() == 0)
			return eaList;
		for (Object[] po : voList) {
			MoneyVO vo = new MoneyVO();
			vo.setMoney(Double.parseDouble("" + po[0]));
			vo.setMoneyType(po[1] + "");
			eaList.add(new MoneyImpl(vo));
		}

		return eaList;
	}

	@Override
	public Collection<Object[]> reportMoneyGroupByYear() {
		Collection<Object[]> voList = this.moneyDao
				.commonSqlGroupByYear(groupByYear);
		return voList;
	}

	@Override
	public Collection<Object[]> reportMoneyGroupByMonth(int year) {
		Collection<Object[]> voList = this.moneyDao.commonSqlGroupByMonth(
				groupByMonth, new Object[] { year });
		return voList;
	}
}
