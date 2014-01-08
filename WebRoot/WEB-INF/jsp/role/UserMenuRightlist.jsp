
<%@ page contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>
<%@ include file="/include.inc.jsp"%>
<style type="text/css">
ul.rightTools {
	float: right;
	display: block;
}

ul.rightTools li {
	float: left;
	display: block;
	margin-left: 5px
}
</style>
<script type="text/javascript">
	var setting = {
		async : {
			enable : true,
			url : "/money/tree!getOrgWithPeopleTree.do",
			autoParam : [ "id", "name" ],
			dataFilter : filter
		},
		callback : {
			onClick : onClick
		}
	};

	function onClick(e, treeId, treeNode) {
		var zTree = $.fn.zTree.getZTreeObj("treeDemo"), nodes = zTree
				.getSelectedNodes(), v = "", v2 = ""; 
		for ( var i = 0, l = nodes.length; i < l; i++) {
			v += nodes[i].name + ",";
			v2 += nodes[i].id + ",";
		}
		if (v.length > 0)
			v = v.substring(0, v.length - 1);
		if (v2.length > 0)
			v2 = v2.substring(0, v2.length - 1);
			alert(v+"----"+v2); 
	}

	function filter(treeId, parentNode, childNodes) {
		if (!childNodes)
			return null;
		for ( var i = 0, l = childNodes.length; i < l; i++) {
			childNodes[i].name = childNodes[i].name.replace(/\.n/g, '.');
		}
		return childNodes;
	}
//-->
</script>
<div class="pageContent" style="padding:5px">
	<div class="tabs">
		<div class="tabsHeader">
			<div class="tabsHeaderContent">
				<ul>
					<li><a href="javascript:;"><span>用户菜单权限</span> </a>
					</li>
					<li><a href="javascript:;"><span>用户角色分配</span> </a>
					</li>
					<li><a href="javascript:;"><span>角色菜单权限</span> </a>
					</li>
				</ul>
			</div>
		</div>
		<div class="tabsContent" style="height:400px">
			<div>
				<div class="zTreeDemoBackground left" style="float:left; display:block; height:400px;overflow:auto; width:240px; border:solid 1px #CCC; line-height:21px; background:#fff">
					<ul id="treeDemo" class="ztree" height='300' lazy="true"></ul>
				</div>

				<div id="jbsxBox" class="unitBox" style="margin-left:246px;">
					<!--#include virtual="list1.html" -->
				</div>
			</div>

			<div>病人处方</div>

			<div>病人服药情况</div>

			<div>基线调查</div>

			<div>随访</div>
		</div>
		<div class="tabsFooter">
			<div class="tabsFooterContent"></div>
		</div>
	</div>
</div>




