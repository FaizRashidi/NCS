        <style type="text/css">
            .cgrid table {
                width: 100%;
            }
            .grid th, .grid td {
                border: solid 1px silver;
                margin: 0px;
            }
            .grid-cell-options {
                width: 10px;
            }
            .grid-row-template {
                display: none;
            }
            .grid-cell input:focus {
                background: #efefef;
                border: 1px solid #a1a1a1;
            }
            .grid-cell {
                display: block;
            }
            .grid-cell form {
                display: block;
            }
        </style>

<br />
<hr />

<div class="form-cell" ${elementMetaData!}>       
    <div class="grid cgrid">
        <table cellspacing="0" class="tablesaw tablesaw-stack" data-tablesaw-mode="stack">
            <thead>
                <tr>
                <#list headers?keys as header>
                    <th>${headers[header]}</th>
                </#list>
                </tr>
            </thead>
            <tbody>
                
                <#list rows as row>
                    <tr class="grid-row">
                    <#list headers?keys as header>
                        <td class="invalid" valign="top">
                            <span class="invalid">${row[header]!?html}</span> 
                        </td>
                    </#list>
                    </tr>
                </#list>
            </tbody>    
        </table>
    </div>
</div>
<script>
    console.log('building audit trail ends');

</script>
